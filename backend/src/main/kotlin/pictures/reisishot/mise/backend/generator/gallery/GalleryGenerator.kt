package pictures.reisishot.mise.backend.generator.gallery

import com.drew.imaging.ImageMetadataReader
import kotlinx.html.*
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.MenuLinkContainerItem
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.insertSubcategoryThumbnail
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


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

    internal var cache = Cache()
    private lateinit var cachePath: Path
    private val menuIemComperator = Comparator.comparing<MenuLinkContainerItem, String> { it.text }

    data class Cache(
        val imageInformationData: MutableMap<FilenameWithoutExtension, InternalImageInformation> =
            mutableMapOf(),
        val categoryInformation: MutableMap<CategoryName, CategoryInformation> = mutableMapOf(),
        val computedTags: MutableMap<TagName, MutableSet<InternalImageInformation>> = mutableMapOf(),
        val computedCategories: MutableMap<CategoryName, MutableSet<FilenameWithoutExtension>> =
            mutableMapOf(),
        val computedSubcategories: MutableMap<CategoryName?, Set<CategoryName>> = mutableMapOf(),
        val computedCategoryThumbnails: MutableMap<CategoryName, InternalImageInformation> = mutableMapOf()
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
                    configuration.tmpPath withChild ThumbnailGenerator.NAME_THUMBINFO_SUBFOLDER withChild "$filenameWithoutExtension.cache.xml"
                if (!configPath.exists())
                    throw IllegalStateException("Config path does not exist for $jpegPath!")
                if (!jpegPath.exists())
                    throw IllegalStateException("Image path does not exist for $jpegPath!")
                if (!thumbnailInfoPath.exists())
                    throw IllegalStateException("Thumbnail Info path does not exist for $jpegPath!")


                val imageConfig: ImageConfig = configPath.parseConfig()
                    ?: throw IllegalStateException("Could not load config file $configPath. Please check if the format is valid!")
                val exifData = jpegPath.readExif()
                val thumbnailConfig: HashMap<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation> =
                    thumbnailInfoPath.fromXml() ?: throw IllegalStateException("Thumbnail info not found...")

                InternalImageInformation(
                    filenameWithoutExtension,
                    imageConfig.title,
                    imageConfig.tags,
                    exifData,
                    thumbnailConfig
                ).apply {
                    cache.imageInformationData.put(filenameWithoutExtension, this)
                    imageConfig.categoryThumbnail.forEach { category ->
                        synchronized(cache.computedCategoryThumbnails) {
                            cache.computedCategoryThumbnails.let { thumbnails ->
                                thumbnails.get(category).let {
                                    if (it != null)
                                        throw IllegalStateException("A thumbnail for $category has already been set! (\"${it.title}\"")
                                    else
                                        thumbnails.put(category, this)
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun buildTags(cache: BuildingCache) = with(this.cache) {
        cache.clearMenuItems { LINKTYPE_TAGS == it.id }
        cache.resetLinkcacheFor(LINKTYPE_TAGS)
        imageInformationData.values.forEach { imageInformation ->
            imageInformation.tags.forEach { tag ->
                computedTags.computeIfAbsent(tag) { mutableSetOf() } += imageInformation
                // Add tag URLs to global cache
                "/gallery/tags/$tag".let { link ->
                    cache.addLinkcacheEntryFor(LINKTYPE_TAGS, tag, link)
                    cache.addMenuItemInContainerNoDupes(LINKTYPE_TAGS, "Tags", 300, tag, link, menuIemComperator)
                }
            }
        }
    }

    private suspend fun buildCategories(
        websiteConfiguration: WebsiteConfiguration,
        cache: BuildingCache
    ) = with(this.cache) {
        val categoryLevelMap: MutableMap<Int, MutableSet<CategoryInformation>> = mutableMapOf()
        cache.clearMenuItems { LINKTYPE_CATEGORIES == it.id }
        cache.resetLinkcacheFor(LINKTYPE_CATEGORIES)
        categoryBuilders.forEach { categoryBuilder ->
            categoryBuilder.generateCategories(this@GalleryGenerator, websiteConfiguration)
                .forEach { (filename, categoryInformation) ->
                    categoryInformation.complexName.let { categoryName ->
                        categoryName.count { it == '/' }.let { subcategoryLevel ->
                            categoryLevelMap.computeIfAbsent(subcategoryLevel) { mutableSetOf() } += categoryInformation
                        }
                        computedCategories.computeIfAbsent(categoryInformation.complexName) {
                            val link = "gallery/categories/${categoryInformation.urlFragment}"
                            cache.addLinkcacheEntryFor(LINKTYPE_CATEGORIES, categoryInformation.complexName, link)
                            if (categoryInformation.visible) {
                                cache.addMenuItemInContainer(
                                    LINKTYPE_CATEGORIES, "Kategorien", 200, categoryInformation.complexName.simpleName,
                                    link, menuIemComperator
                                )
                            }
                            mutableSetOf()
                        } += filename

                        imageInformationData[filename]?.categories?.add(categoryInformation)
                        this.categoryInformation.computeIfAbsent(categoryInformation.complexName) { categoryInformation }
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
                        }.map { it.complexName }
                        .toSet().let { subcategories ->
                            if (subcategories.isNotEmpty())
                                computedSubcategories.put(category.complexName, subcategories)
                        }
                }
            }
        }

        // Add first level subcategories
        categoryLevelMap[0]?.asSequence()?.map { it.complexName }?.toSet()?.let { firstLevelCategories ->
            computedSubcategories.put(null, firstLevelCategories)
        }
    }

    private suspend fun generateWebpages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        generateImagePages(configuration, cache)
        generateCategoryPages(configuration, cache)
        generateTagPages(configuration, cache)
    }

    private suspend fun generateImagePages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("eeee 'den' dd.MM.YYYY 'um' HH:mm:ss z")
        (configuration.outPath withChild "gallery/images").let { baseHtmlPath ->
            this.cache.imageInformationData.values.forEachLimitedParallel(50) { curImageInformation ->
                PageGenerator.generatePage(
                    websiteConfiguration = configuration,
                    buildingCache = cache,
                    target = baseHtmlPath withChild curImageInformation.url withChild "index.html",
                    title = curImageInformation.title,
                    pageContent = {
                        classes = classes + "singleImage"
                        h1("text-center") {
                            text(curImageInformation.title)
                        }
                        insertImageGallery("1", curImageInformation)
                        div("card") {
                            h4("card-title") {
                                text("Exif Informationen")
                            }
                            div("card-body") {
                                div("card-text") {
                                    div("container") {
                                        curImageInformation.exifInformation.forEach { (type, value) ->
                                            div("row justify-content-between") {
                                                div("col-3 align-self-center") {
                                                    text("${type.displayName}:")
                                                }
                                                div("col-9 align-self-center") {
                                                    when (type) {
                                                        ExifdataKey.CREATION_TIME -> text(
                                                            ZonedDateTime.parse(value).format(
                                                                dateTimeFormatter
                                                            )
                                                        )
                                                        else -> text(value)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private val ExifdataKey.displayName
        get() = when (this) {
            ExifdataKey.CREATION_TIME -> "Erstellt am"
            ExifdataKey.LENS_MODEL -> "Objektiv"
            ExifdataKey.FOCAL_LENGTH -> "Brennweite"
            ExifdataKey.APERTURE -> "Blende"
            ExifdataKey.CAMERA_MAKE -> "Kamerahersteller"
            ExifdataKey.CAMERA_MODEL -> "Kameramodell"
            ExifdataKey.ISO -> "ISO"
            ExifdataKey.SHUTTER_SPEED -> "Verschlusszeit"
        }

    private fun generateCategoryPages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) = with(this.cache) {
        (configuration.outPath withChild "gallery/categories").let { baseHtmlPath ->
            computedCategories.forEach { (categoryName, categoryImages) ->
                val categoryMetaInformation = categoryInformation[categoryName]
                    ?: throw IllegalStateException("No category information found for name \"$categoryName\"!")
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

                        insertSubcategoryThumbnails(categoryMetaInformation.complexName)

                        insertImageGallery("1", *imageInformations)
                    }
                )
            }
        }
    }

    fun DIV.insertSubcategoryThumbnails(categoryName: CategoryName?) =
        insertSubcategoryThumbnails(categoryName, this@GalleryGenerator)

    private fun generateTagPages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) = with(this.cache) {
        (configuration.outPath withChild "gallery/tags").let { baseHtmlPath ->
            computedTags.forEach { (tagName, tagImages) ->
                val targetFile = baseHtmlPath withChild tagName withChild "index.html"

                PageGenerator.generatePage(
                    websiteConfiguration = configuration,
                    buildingCache = cache,
                    target = targetFile,
                    title = tagName,
                    pageContent = {
                        h1("text-center") {
                            text("Tag - ")
                            i {
                                text(("\"$tagName\""))
                            }
                        }

                        insertImageGallery(tagName, *tagImages.toTypedArray())
                    })
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

fun DIV.insertSubcategoryThumbnails(categoryName: CategoryName?, generator: GalleryGenerator) = with(generator.cache) {
    val subcategories = computedSubcategories[categoryName]
    if (!subcategories.isNullOrEmpty())
        div("subcategories") {
            subcategories.asSequence()
                .map {
                    categoryInformation.getValue(it) to
                            computedCategoryThumbnails.getThumbnailImageInformation(it, generator)
                }
                .filterNotNull()
                .sortedBy { (categoryInformation, _) -> categoryInformation.complexName }
                .forEach { (categoryName, imageInformation) ->
                    if (imageInformation != null)
                        insertSubcategoryThumbnail(
                            categoryName,
                            imageInformation
                        )
                }
        }
}

internal fun Map<CategoryName, InternalImageInformation>.getThumbnailImageInformation(
    category: CategoryName,
    generator: GalleryGenerator
): InternalImageInformation? =
    get(category) ?: generator.cache.imageInformationData[generator.cache.computedCategories[category]?.first()]
    ?: throw IllegalStateException("Could not find thumbnail for \"$category\"!")