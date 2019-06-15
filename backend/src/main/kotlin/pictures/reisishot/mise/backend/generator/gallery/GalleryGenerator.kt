package pictures.reisishot.mise.backend.generator.gallery

import com.drew.imaging.ImageMetadataReader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Path

@ObsoleteCoroutinesApi
class GalleryGenerator(
    private vararg val categoryBuilders: CategoryBuilder,
    private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : WebsiteGenerator,
    ImageInformationRepository {

    override val generatorName: String = "Reisishot Gallery"
    override val executionPriority: Int = 20_000

    private val cache = Cache()

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


    override suspend fun generate(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        val thumbnailGenerator = alreadyRunGenerators.find { it is ThumbnailGenerator } as? ThumbnailGenerator
            ?: throw IllegalStateException("Thumbnail generator has bot run yet, however this is needed for this generator")
        buildImageInformation(thumbnailGenerator)
        buildTags()
        buildCategories(configuration)

        generateWebpages()
    }

    private suspend fun buildImageInformation(thumbnailGenerator: ThumbnailGenerator) {
        thumbnailGenerator.imageFolder.list().filter { it.isJpeg }.asIterable().forEachLimitedParallel(20) { jpegPath ->
            val filenameWithoutExtension = jpegPath.filenameWithoutExtension
            val configPath = jpegPath.parent withChild filenameWithoutExtension + ".conf"
            if (configPath.exists() && jpegPath.exists()) {
                val imageConfig: ImageConfig = configPath.parseConfig()
                    ?: throw IllegalStateException("Could not load config file $configPath. Please check if the format is valid!")
                val exifData = jpegPath.readExif()

                InternalImageInformation(
                    jpegPath,
                    filenameWithoutExtension,
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

    fun generateWebpages() {
        // TODO implement
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

    override suspend fun setup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.setup(configuration, cache)
        categoryBuilders.forEach { it.setup(configuration, cache) }
    }

    override suspend fun teardown(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.teardown(configuration, cache)
        categoryBuilders.forEach { it.teardown(configuration, cache) }
    }
}