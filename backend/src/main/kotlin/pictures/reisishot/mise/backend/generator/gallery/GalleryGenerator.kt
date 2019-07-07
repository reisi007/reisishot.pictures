package pictures.reisishot.mise.backend.generator.gallery

import com.drew.imaging.ImageMetadataReader
import kotlinx.html.h1
import kotlinx.html.i
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.MenuLinkContainer
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.html.PageGenerator
import java.nio.file.Path


class GalleryGenerator(
    private vararg val categoryBuilders: CategoryBuilder,
    private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : WebsiteGenerator,
    ImageInformationRepository {

    companion object {
        const val LINKTYPE_TAGS = "TAGS"
        const val LINKTYPE_CATEGORIES = "CATEGORIES"
    }

    override val generatorName: String = "Reisishot Gallery"
    override val executionPriority: Int = 20_000

    private var cache = Cache()
    private lateinit var cachePath: Path

    data class Cache(
        val imageInformationData: MutableMap<FilenameWithoutExtension, InternalImageInformation> =
            mutableMapOf(),
        val computedTags: MutableMap<TagName, MutableSet<InternalImageInformation>> = mutableMapOf(),
        val computedCategories: MutableMap<CategoryInformation, MutableSet<FilenameWithoutExtension>> =
            mutableMapOf(),
        val computedSubcategories: MutableMap<CategoryInformation, Set<CategoryInformation>> = mutableMapOf()
    )

    override val imageInformationData: Collection<ImageInformation> = cache.imageInformationData.values
    override val computedTags: Map<TagName, Set<ImageInformation>> = cache.computedTags


    override suspend fun fetchInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) = buildCache(configuration, cache)

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) =
        generateWebpages(configuration, cache)


    private suspend fun buildCache(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        val newestFile = configuration.inPath.withChild(ThumbnailGenerator.NAME_IMAGE_SUBFOLDER).list()
            .map {
                it.fileModifiedDateTime
                    ?: throw IllegalStateException("File $it is listed but no file modified time...")
            }.max() ?: return  // No file to detect

        cachePath.fileModifiedDateTime?.let { cacheTime ->
            if (cacheTime > newestFile) {
                this.cache = cachePath.fromXml()
                    ?: throw IllegalStateException("Cache has a modifed date, but cannot be parsed!")
                return
            }
        }

        buildImageInformation(configuration)
        buildTags(cache)
        buildCategories(configuration, cache)
    }

    // TODO Write cache files and use them instead of computing...
    private suspend fun buildImageInformation(configuration: WebsiteConfiguration) {
        (configuration.inPath withChild ThumbnailGenerator.NAME_IMAGE_SUBFOLDER).list().filter { it.isJpeg }
            .asIterable()
            .forEachLimitedParallel(20) { jpegPath ->
                val filenameWithoutExtension = jpegPath.filenameWithoutExtension
                val configPath = jpegPath.parent withChild "$filenameWithoutExtension.conf"
                val thumbnailInfoPath =
                    configuration.inPath withChild ThumbnailGenerator.NAME_THUMBINFO_SUBFOLDER withChild "$filenameWithoutExtension.xml"
                if (configPath.exists() && jpegPath.exists() && thumbnailInfoPath.exists()) {
                    val imageConfig: ImageConfig = configPath.parseConfig()
                        ?: throw IllegalStateException("Could not load config file $configPath. Please check if the format is valid!")
                    val exifData = jpegPath.readExif()
                    val thumbnailConfig: HashMap<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation> =
                        thumbnailInfoPath.fromXml() ?: throw IllegalStateException("Thumbnail info not found...")

                    InternalImageInformation(
                        filenameWithoutExtension,
                        imageConfig.url,
                        imageConfig.title,
                        imageConfig.tags,
                        exifData,
                        thumbnailConfig
                    ).apply {
                        cache.imageInformationData.put(filenameWithoutExtension, this)
                    }
                }
            }
    }

    private fun buildTags(cache: BuildingCache) = with(this.cache) {
        imageInformationData.values.forEach { imageInformation ->
            imageInformation.tags.forEach { tag ->
                computedTags.computeIfAbsent(tag) { mutableSetOf() } += imageInformation
                // Add tag URLs to global cache
                cache.addLinkcacheEntryFor(LINKTYPE_TAGS, tag, "/gallery/tags/$tag")
            }
        }
    }

    private suspend fun buildCategories(
        websiteConfiguration: WebsiteConfiguration,
        cache: BuildingCache
    ) = with(this.cache) {
        val categoryLevelMap: MutableMap<Int, MutableSet<CategoryInformation>> = mutableMapOf()
        cache.clearMenuItems { it is MenuLinkContainer && LINKTYPE_CATEGORIES == it.containerId }
        categoryBuilders.forEach { categoryBuilder ->
            categoryBuilder.generateCategories(this@GalleryGenerator, websiteConfiguration)
                .forEach { (filename, categoryInformation) ->
                    categoryInformation.complexName.let { categoryName ->
                        categoryName.count { it == '/' }.let { subcategoryLevel ->
                            categoryLevelMap.computeIfAbsent(subcategoryLevel) { mutableSetOf() } += categoryInformation
                        }
                        computedCategories.computeIfAbsent(categoryInformation) {
                            if (categoryInformation.visible) {
                                cache.addMenuItem(
                                    LINKTYPE_CATEGORIES, "Kategorien", 2, categoryInformation.complexName.simpleName,
                                    "/gallery/categories/${categoryInformation.urlFragment}"
                                )
                            }
                            mutableSetOf()
                        } += filename

                        imageInformationData[filename]?.categories?.add(categoryInformation) //TODO check for eleted images
                    }
                }
        }

        categoryLevelMap.keys.forEach { level ->
            val nextLevel = level + 1
            categoryLevelMap.getValue(level).forEach { category ->
                categoryLevelMap[nextLevel]?.let { possibleSubcategories ->
                    possibleSubcategories.asSequence()
                        .filter { possibleSubcategory ->
                            possibleSubcategory.complexName.startsWith(
                                category.complexName,
                                true
                            )
                        }
                        .toSet().let { subcategories ->
                            if (subcategories.isNotEmpty())
                                computedSubcategories.put(category, subcategories)
                        }
                }
            }
        }
        // Add tag URLs to global cache
        categoryLevelMap[0]?.forEach { cur ->
            addCategoryLinkFor(cur, cache, "/gallery/categories")
        }
    }

    private fun addCategoryLinkFor(curCategory: CategoryInformation, cache: BuildingCache, prefix: String): Unit =
        with(this.cache) {
            val categoryUrl = "$prefix/${curCategory.urlFragment}"
            cache.addLinkcacheEntryFor(LINKTYPE_CATEGORIES, curCategory.complexName, categoryUrl)
            computedSubcategories[curCategory]?.forEach { nextCategory ->
                addCategoryLinkFor(nextCategory, cache, categoryUrl)
            }
        }

    private suspend fun generateWebpages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        generateImagePages(configuration, cache)
        generateCategoryPages(configuration, cache)
    }

    private suspend fun generateImagePages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        (configuration.outPath withChild "gallery/images").let { baseHtmlPath ->
            this.cache.imageInformationData.values.forEachLimitedParallel(50) { curImageInformation ->
                PageGenerator.generatePage(
                    websiteConfiguration = configuration,
                    buildingCache = cache,
                    target = baseHtmlPath withChild curImageInformation.url withChild "index.html",
                    title = curImageInformation.title,
                    pageContent = {
                        h1("text-center") {
                            text(curImageInformation.title)
                        }
                        insertImageGallery("1", curImageInformation)
                    }
                )
            }
        }
    }

    private fun generateCategoryPages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) = with(this.cache) {
        (configuration.outPath withChild "gallery/categories").let { baseHtmlPath ->
            computedCategories.forEach { (categoryMetaInformation, categoryImages) ->
                val targetFile = baseHtmlPath withChild categoryMetaInformation.urlFragment withChild "index.html"
                PageGenerator.generatePage(
                    websiteConfiguration = configuration,
                    buildingCache = cache,
                    target = targetFile,
                    title = categoryMetaInformation.complexName.simpleName,
                    pageContent = {
                        h1("text-center") {
                            text("Kategorie - ")
                            i {
                                text(("\"${categoryMetaInformation.complexName.simpleName}\""))
                            }
                        }

                        val imageInformations = categoryImages.asSequence()
                            .map { imageInformationData.getValue(it) }
                            .toArray(categoryImages.size)

                        insertImageGallery("1", *imageInformations)
                    }
                )
            }
        }
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
        cachePath = configuration.tmpPath withChild "gallery.cache.xml"
        categoryBuilders.forEach { it.setup(configuration, cache) }
    }

    override suspend fun saveCache(configuration: WebsiteConfiguration, buildingCache: BuildingCache) {
        super.saveCache(configuration, buildingCache)
        cache.toXml(cachePath)
        categoryBuilders.forEach { it.teardown(configuration, buildingCache) }
    }
}