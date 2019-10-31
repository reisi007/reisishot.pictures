package pictures.reisishot.mise.backend.generator.gallery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.*
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.Companion.NAME_IMAGE_SUBFOLDER
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.Companion.NAME_THUMBINFO_SUBFOLDER
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ThumbnailInformation
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.insertSubcategoryThumbnail
import pictures.reisishot.mise.backend.html.smallButtonLink
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap


class GalleryGenerator(
        private vararg val categoryBuilders: CategoryBuilder,
        private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : WebsiteGenerator, ImageInformationRepository {

    companion object {
        const val LINKTYPE_TAGS = "TAGS"
        const val LINKTYPE_CATEGORIES = "CATEGORIES"
        const val SUBFOLDER_OUT = "gallery/images"
    }

    override val generatorName: String = "Reisishot Gallery"
    override val executionPriority: Int = 20_000

    internal var cache = Cache()
    private lateinit var cachePath: Path
    private val menuIemComperator = Comparator.comparing<MenuLinkContainerItem, String> { it.text }

    data class Cache(
            val imageInformationData: MutableMap<FilenameWithoutExtension, InternalImageInformation> = concurrentSkipListMap(),
            val categoryInformation: MutableMap<CategoryName, CategoryInformation> = concurrentSkipListMap(),
            val computedTags: MutableMap<TagInformation, MutableSet<InternalImageInformation>> = concurrentSkipListMap(compareBy(TagInformation::name)),
            val computedCategories: MutableMap<CategoryName, MutableSet<FilenameWithoutExtension>> = concurrentSkipListMap(),
            val computedSubcategories: MutableMap<CategoryName, Set<CategoryName>> = concurrentSkipListMap(),
            val computedCategoryThumbnails: MutableMap<CategoryName, InternalImageInformation> = concurrentSkipListMap()
    ) {
        companion object {
            private fun <K, V> concurrentSkipListMap(comparator: Comparator<in K>): MutableMap<K, V> = ConcurrentSkipListMap(comparator)
            private fun <K : Comparable<K>, V> concurrentSkipListMap(): MutableMap<K, V> = ConcurrentSkipListMap()
        }
    }


    override val imageInformationData: Collection<ImageInformation> = cache.imageInformationData.values
    override val computedTags: Map<TagInformation, Set<ImageInformation>> = cache.computedTags


    override suspend fun fetchInitialInformation(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            alreadyRunGenerators: List<WebsiteGenerator>
    ) = buildCache(configuration, cache)

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) =
            generateWebpages(configuration, cache)


    private suspend fun buildCache(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    ) {
        val newestFile = configuration.inPath.withChild(NAME_IMAGE_SUBFOLDER).list()
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
        (configuration.inPath withChild NAME_IMAGE_SUBFOLDER).list().filter { it.fileExtension.isJpeg() }
                .asIterable()
                .forEachLimitedParallel(20) { jpegPath ->
                    val filenameWithoutExtension = jpegPath.filenameWithoutExtension
                    val configPath = jpegPath.parent withChild "$filenameWithoutExtension.conf"
                    val thumbnailInfoPath =
                            configuration.tmpPath withChild NAME_THUMBINFO_SUBFOLDER withChild "$filenameWithoutExtension.cache.xml"
                    if (!configPath.exists())
                        throw IllegalStateException("Config path does not exist for $jpegPath!")
                    if (!jpegPath.exists())
                        throw IllegalStateException("Image path does not exist for $jpegPath!")
                    if (!thumbnailInfoPath.exists())
                        throw IllegalStateException("Thumbnail Info path does not exist for $jpegPath!")


                    val imageConfig: ImageConfig = configPath.parseConfig()
                            ?: throw IllegalStateException("Could not load config file $configPath. Please check if the format is valid!")
                    val exifData = jpegPath.readExif(exifReplaceFunction)
                    val thumbnailConfig: HashMap<ImageSize, ThumbnailInformation> =
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
            imageInformation.tags.forEach { tagName ->
                val tag = TagInformation(tagName)
                computedTags.computeIfAbsent(tag) { mutableSetOf() } += imageInformation
                // Add tag URLs to global cache
                "gallery/tags/${tag.url}".let { link ->
                    cache.addLinkcacheEntryFor(LINKTYPE_TAGS, tag.name, link)
                    cache.addMenuItemInContainerNoDupes(LINKTYPE_TAGS, "Tags", 300, tag.name, link, menuIemComperator)
                }
            }
        }
    }

    private suspend fun buildCategories(
            websiteConfiguration: WebsiteConfiguration,
            cache: BuildingCache
    ) = with(this.cache) {
        val categoryLevelMap: MutableMap<Int, MutableSet<CategoryInformation>> = ConcurrentHashMap()
        cache.clearMenuItems { LINKTYPE_CATEGORIES == it.id }
        cache.resetLinkcacheFor(LINKTYPE_CATEGORIES)

        categoryBuilders.forEach { categoryBuilder ->
            categoryBuilder.generateCategories(this@GalleryGenerator, websiteConfiguration)
                    .forEach { (filename, categoryInformation) ->
                        categoryInformation.internalName.let { categoryName ->
                            categoryName.complexName.count { it == '/' }.let { subcategoryLevel ->
                                categoryLevelMap.computeIfAbsent(subcategoryLevel) { mutableSetOf() } += categoryInformation
                            }
                            computedCategories.computeIfAbsent(categoryInformation.internalName) {
                                val link = "gallery/categories/${categoryInformation.urlFragment}"
                                cache.addLinkcacheEntryFor(LINKTYPE_CATEGORIES, categoryInformation.complexName, link)
                                if (categoryInformation.visible) {
                                    cache.addMenuItemInContainer(
                                            LINKTYPE_CATEGORIES, "Kategorien", 200, categoryInformation.displayName,
                                            link, menuIemComperator
                                    )
                                }
                                mutableSetOf()
                            } += filename

                            imageInformationData[filename]?.categories?.add(categoryInformation)
                            this.categoryInformation.computeIfAbsent(categoryInformation.internalName) { categoryInformation }
                        }
                    }
        }

        categoryLevelMap.values.asSequence()
                .flatMap { it.asSequence() }
                .map { it.internalName to it.subcategoryComputator(categoryLevelMap) }
                .forEach { (category, subcategories) ->
                    computedSubcategories.put(category, subcategories)
                }

        // Add first level subcategories
        categoryLevelMap[0]?.asSequence()
                ?.filter { it.visible }
                ?.map { it.internalName }
                ?.filter { it.complexName.isNotBlank() }
                ?.toSet()
                ?.let { firstLevelCategories ->
                    computedSubcategories.put(CategoryName(""), firstLevelCategories)
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
                        target = baseHtmlPath withChild curImageInformation.url.toLowerCase() withChild "index.html",
                        title = curImageInformation.title,
                        pageContent = {
                            classes = classes + "singleImage"
                            h1("text-center") {
                                text(curImageInformation.title)
                            }
                            insertImageGallery("1", curImageInformation)

                            insertCategoryLinks(curImageInformation, configuration, cache)

                            insertTagLinks(curImageInformation, configuration, cache)

                            insertExifInformation(curImageInformation, dateTimeFormatter)
                        }
                )
            }
        }
    }

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changedFiles: ChangedFileset) {
        if (changedFiles.hasRelevantChanges()) {
            cleanup(configuration, cache)
            fetchInitialInformation(configuration, cache, alreadyRunGenerators)
        }
    }

    override suspend fun buildUpdateArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache, changedFiles: ChangedFileset) {
        if (changedFiles.hasRelevantChanges())
            buildInitialArtifacts(configuration, cache)
    }

    private fun ChangedFileset.hasRelevantChanges() =
            keys.asSequence()
                    .any { it.hasExtension(FileExtension::isConf) }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache): Unit {
        withContext(Dispatchers.IO) {
            Files.list(configuration.outPath.resolve(SUBFOLDER_OUT))
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::delete)
        }
    }

    private fun DIV.insertTagLinks(curImageInformation: InternalImageInformation, configuration: WebsiteConfiguration, cache: BuildingCache) {
        div("card") {
            h4("card-title") {
                text("Tags")
            }
            div("card-body btn-flex") {
                curImageInformation.tags.forEach { category ->
                    smallButtonLink(category, configuration.websiteLocation + cache.getLinkcacheEntryFor(LINKTYPE_TAGS, category))
                }
            }
        }
    }

    private fun DIV.insertCategoryLinks(curImageInformation: InternalImageInformation, configuration: WebsiteConfiguration, cache: BuildingCache) {
        div("card") {
            h4("card-title") {
                text("Kategorien")
            }
            div("card-body btn-flex") {
                curImageInformation.categories.forEach { category ->
                    smallButtonLink(category.displayName, configuration.websiteLocation + cache.getLinkcacheEntryFor(LINKTYPE_CATEGORIES, category.complexName))
                }
            }
        }
    }

    private fun DIV.insertExifInformation(curImageInformation: InternalImageInformation, dateTimeFormatter: DateTimeFormatter) {
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
                        title = categoryMetaInformation.displayName,
                        pageContent = {
                            h1("text-center") {
                                text("Kategorie - ")
                                i {
                                    text(("\"${categoryMetaInformation.displayName}\""))
                                }
                            }

                            val imageInformations = with(categoryImages) {
                                asSequence()
                                        .map { imageInformationData.getValue(it) }
                                        .toOrderedByTimeArray(size)
                            }

                            insertSubcategoryThumbnails(categoryMetaInformation.internalName)

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
                val targetFile = baseHtmlPath withChild tagName.url withChild "index.html"

                PageGenerator.generatePage(
                        websiteConfiguration = configuration,
                        buildingCache = cache,
                        target = targetFile,
                        title = tagName.name,
                        pageContent = {
                            h1("text-center") {
                                text("Tag - ")
                                i {
                                    text(("\"${tagName.name}\""))
                                }
                            }

                            insertImageGallery("1", *tagImages.toOrderedByTimeArray())
                        })
            }
        }
    }

    fun Collection<InternalImageInformation>.toOrderedByTimeArray() = asSequence().toOrderedByTimeArray(size)
    fun Sequence<InternalImageInformation>.toOrderedByTimeArray(size: Int) = sortedByDescending { it.exifInformation[ExifdataKey.CREATION_TIME] }
            .toArray(size)


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
        name: CategoryName,
        generator: GalleryGenerator
): InternalImageInformation? =
        get(name)
                ?: generator.cache.imageInformationData[generator.cache.computedCategories[name]?.first()]
                ?: throw IllegalStateException("Could not find thumbnail for \"$name\"!")
