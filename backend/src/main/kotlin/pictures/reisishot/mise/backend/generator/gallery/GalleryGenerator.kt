package pictures.reisishot.mise.backend.generator.gallery

import com.drew.imaging.ImageMetadataReader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.PageGenerator.singleImageGallery
import java.nio.file.Path

@ObsoleteCoroutinesApi
class GalleryGenerator(
    private vararg val categoryBuilders: CategoryBuilder,
    private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : WebsiteGenerator,
    ImageInformationRepository {

    override val generatorName: String = "Reisishot Gallery"
    override val executionPriority: Int = 20_000

    private var cache = Cache()
    private lateinit var cachePath: Path

    data class Cache(
        val imageInformationData: MutableMap<FilenameWithoutExtension, InternalImageInformation> =
            mutableMapOf(),
        val computedTags: MutableMap<TagName, MutableSet<InternalImageInformation>> = mutableMapOf(),
        val computedCategories: MutableMap<CategoryName, MutableSet<FilenameWithoutExtension>> =
            mutableMapOf(),
        val computedSubcategories: MutableMap<CategoryName, Set<CategoryName>> = mutableMapOf()
    )

    override val imageInformationData: Collection<ImageInformation> = cache.imageInformationData.values
    override val computedTags: Map<TagName, Set<ImageInformation>> = cache.computedTags


    override suspend fun fetchInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) = buildCache(configuration)

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) =
        generateWebpages(configuration)


    private suspend fun buildCache(configuration: WebsiteConfiguration) {
        val newestFile = configuration.inPath.withChild(ThumbnailGenerator.NAME_SUBFOLDER).list()
            .map {
                it.fileModifiedDateTime
                    ?: throw IllegalStateException("File $it is listed but no file modified time...")
            }.max() ?: return  // No file to detect

        cachePath.fileModifiedDateTime?.let { cacheTime ->
            if (cacheTime > newestFile) {
                cache = cachePath.fromJson()
                    ?: throw IllegalStateException("Cache has a modifed date, but cannot be parsed!")
                return
            }
        }

        buildImageInformation(configuration)
        buildTags()
        buildCategories(configuration)
    }

    // TODO Write cache files and use them instead of computing...
    private suspend fun buildImageInformation(configuration: WebsiteConfiguration) {
        (configuration.inPath withChild ThumbnailGenerator.NAME_SUBFOLDER).list().filter { it.isJpeg }.asIterable()
            .forEachLimitedParallel(20) { jpegPath ->
                val filenameWithoutExtension = jpegPath.filenameWithoutExtension
                val configPath = jpegPath.parent withChild filenameWithoutExtension + ".conf"
                if (configPath.exists() && jpegPath.exists()) {
                    val imageConfig: ImageConfig = configPath.parseConfig()
                        ?: throw IllegalStateException("Could not load config file $configPath. Please check if the format is valid!")
                    val exifData = jpegPath.readExif()

                    InternalImageInformation(
                        jpegPath,
                        filenameWithoutExtension,
                        imageConfig.url,
                        imageConfig.title,
                        imageConfig.tags,
                        exifData
                    ).apply {
                        cache.imageInformationData.put(filenameWithoutExtension, this)
                    }
                }
            }
    }

    private fun buildTags() = with(cache) {
        imageInformationData.values.forEach { imageInformation ->
            imageInformation.tags.forEach { tag ->
                computedTags.computeIfAbsent(tag) { mutableSetOf() } += imageInformation
            }
        }
    }

    private suspend fun buildCategories(websiteConfiguration: WebsiteConfiguration) = with(cache) {
        val categoryLevelMap: MutableMap<Int, MutableSet<CategoryName>> = mutableMapOf()

        categoryBuilders.forEach { categoryBuilder ->
            categoryBuilder.generateCategories(this@GalleryGenerator, websiteConfiguration)
                .forEach { (filename, categoryName) ->
                    categoryName.count { it == '/' }.let { subcategoryLevel ->
                        categoryLevelMap.computeIfAbsent(subcategoryLevel) { mutableSetOf() } += categoryName
                    }
                    computedCategories.computeIfAbsent(categoryName) { mutableSetOf() } += filename
                    imageInformationData[filename]?.categories?.add(categoryName) ?: throw IllegalStateException(
                        "Could not add $categoryName to Image with filename $filename!"
                    )
                }
        }

        categoryLevelMap.keys.forEach { level ->
            val nextLevel = level + 1
            categoryLevelMap.getValue(level).forEach { category ->
                categoryLevelMap[nextLevel]?.let { possibleSubcategories ->
                    possibleSubcategories.asSequence()
                        .filter { possibleSubcategory -> possibleSubcategory.startsWith(category, true) }
                        .toSet().let { subcategories ->
                            if (subcategories.isNotEmpty())
                                computedSubcategories.put(category, subcategories)
                        }
                }
            }
        }
    }

    private suspend fun generateWebpages(configuration: WebsiteConfiguration) {
        val generatedImagesPath = configuration.outPath withChild ThumbnailGenerator.NAME_SUBFOLDER
        (configuration.outPath withChild "gallery/images").let { baseHtmlPath ->
            cache.imageInformationData.values.forEachLimitedParallel(50) { (_, imageName, url, title, tags, exifInformation, categories) ->
                PageGenerator.generatePage(
                    target = baseHtmlPath withChild url withChild "index.html",
                    title = title,
                    pageContent = {
                        singleImageGallery(generatedImagesPath, imageName)
                    }
                )
            }
        }
        //TODO implement other and extract to other functions for betetr readability
    }

    private fun Path.readExif(): Map<ExifdataKey, String> = mutableMapOf<ExifdataKey, String>().apply {
        ExifInformation(ImageMetadataReader.readMetadata(this@readExif.toFile()))
            .let { exifInformation ->
                ExifdataKey.values().forEach { key ->
                    val exifValue = key.getValue(exifInformation)
                    exifReplaceFunction(key to exifValue)
                        .also { (key, possibleValue) ->
                            if (possibleValue != null)
                                put(key, possibleValue)
                        }
                }
            }
    }

    override suspend fun loadCache(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.loadCache(configuration, cache)
        cachePath = configuration.inPath withChild "gallery.cache.json"
        categoryBuilders.forEach { it.setup(configuration, cache) }
    }

    override suspend fun saveCache(configuration: WebsiteConfiguration, buildingCache: BuildingCache) {
        super.saveCache(configuration, buildingCache)
        cache.toJson(cachePath)
        categoryBuilders.forEach { it.teardown(configuration, buildingCache) }
    }
}