package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.*
import at.reisishot.mise.config.ImageConfig
import at.reisishot.mise.exifdata.ExifdataKey
import at.reisishot.mise.exifdata.readExif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.DIV
import kotlinx.html.HEAD
import kotlinx.html.div
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.config.category.*
import pictures.reisishot.mise.backend.config.tags.TagConfig
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.backend.config.tags.computeTags
import pictures.reisishot.mise.backend.fromJson
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.generator.pages.IPageMininmalInfo
import pictures.reisishot.mise.backend.generator.pages.minimalistic.SourcePath
import pictures.reisishot.mise.backend.generator.pages.minimalistic.TargetPath
import pictures.reisishot.mise.backend.generator.testimonials.TestimonialLoader
import pictures.reisishot.mise.backend.generator.testimonials.findTestimonialLoader
import pictures.reisishot.mise.backend.html.insertSubcategoryThumbnail
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import pictures.reisishot.mise.backend.toJson
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.streams.asSequence


abstract class AbstractGalleryGenerator(
    private val tagConfig: TagConfig,
    private val categoryConfig: CategoryConfigRoot,
    private val displayedMenuItems: Set<DisplayedMenuItems> = setOf(
        DisplayedMenuItems.CATEGORIES,
        DisplayedMenuItems.TAGS
    ),
    private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : WebsiteGenerator {

    enum class DisplayedMenuItems {
        CATEGORIES,
        TAGS;
    }

    override val executionPriority: Int = 20_000

    var cache = Cache()
    protected lateinit var testimonialLoader: TestimonialLoader
    private lateinit var cachePath: Path
    protected val ExifdataKey.displayName
        get() = when (this) {
            ExifdataKey.CREATION_DATETIME -> "Erstellt am"
            ExifdataKey.LENS_MODEL -> "Objektiv"
            ExifdataKey.FOCAL_LENGTH -> "Brennweite"
            ExifdataKey.APERTURE -> "Blende"
            ExifdataKey.CAMERA_MAKE -> "Kamerahersteller"
            ExifdataKey.CAMERA_MODEL -> "Kameramodell"
            ExifdataKey.ISO -> "ISO"
            ExifdataKey.SHUTTER_SPEED -> "Verschlusszeit"
        }

    @Serializable
    data class Cache(
        val imageInformationData: MutableMap<FilenameWithoutExtension, ImageInformation> = concurrentSkipListMap(),
        val computedTags: MutableMap<TagInformation, MutableSet<ImageInformation>> = concurrentSkipListMap(
            compareBy(
                TagInformation::name
            )
        ),
        var categoryInformation: CategoryInformationRoot? = null,
        val computedSubcategories: MutableMap<CategoryName, Set<CategoryName>> = concurrentSkipListMap(),
        val computedCategoryThumbnails: MutableMap<CategoryName, ImageInformation> = concurrentSkipListMap()
    ) {
        companion object {
            private fun <K, V> concurrentSkipListMap(comparator: Comparator<in K>): MutableMap<K, V> =
                ConcurrentSkipListMap(comparator)

            private fun <K : Comparable<K>, V> concurrentSkipListMap(): MutableMap<K, V> = ConcurrentSkipListMap()
        }
    }

    companion object {
        const val LINKTYPE_TAGS = "TAGS"
        const val LINKTYPE_CATEGORIES = "CATEGORIES"
        const val SUBFOLDER_OUT = "gallery/images"
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        buildCache(configuration, cache)
        testimonialLoader = alreadyRunGenerators.findTestimonialLoader()
    }

    private suspend fun buildCache(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        val cacheTime = cachePath.fileModifiedDateTime
            ?: kotlin.run { ZonedDateTime.of(LocalDate.of(1900, 1, 1), LocalTime.MIN, ZoneId.systemDefault()) }

        val cacheStillValid = cachePath.exists() &&
                configuration.inPath.withChild(AbstractThumbnailGenerator.NAME_IMAGE_SUBFOLDER)
                    .list()
                    .map { it.fileModifiedDateTime }
                    .filterNotNull()
                    .all { it < cacheTime }

        if (cacheStillValid) {
            this.cache = cachePath.fromJson()
                ?: throw IllegalStateException("Cache has a modified date, but cannot be parsed!")
        }

        val recomputeGallery =
            !cacheStillValid
                    || hasTextChanges(configuration, cacheTime)
                    || hasConfigChanges(configuration, cacheTime)

        if (!recomputeGallery) return

        buildImageInformation(configuration)
        buildTags(cache)
        buildCategories(configuration, cache)
    }

    private fun hasConfigChanges(configuration: WebsiteConfiguration, cacheTime: ZonedDateTime): Boolean {
        return sequenceOf(
            "categories.json",
            "tags.json"
        ).map { configuration.inPath withChild it }
            .map { it.fileModifiedDateTime }
            .filterNotNull()
            .any { it >= cacheTime }
    }

    private fun hasTextChanges(configuration: WebsiteConfiguration, cacheTime: ZonedDateTime): Boolean {
        // A file has been modified, which is newer than the cache
        val path = configuration.inPath withChild "gallery"
        if (!path.exists())
            return false
        return Files.walk(path)
            .asSequence()
            .map { it.fileModifiedDateTime }
            .filterNotNull()
            .any { it >= cacheTime }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) =
        generateWebpages(configuration, cache, testimonialLoader)

    private fun buildTags(cache: BuildingCache) = with(this.cache) {
        val internalImageInformation = imageInformationData.values.asSequence()
            .map { it as? InternalImageInformation }
            .filterNotNull()
            .toList()
        this@AbstractGalleryGenerator.tagConfig.computeTags(internalImageInformation)

        val shallAddToMenu = displayedMenuItems.contains(DisplayedMenuItems.TAGS)
        cache.clearMenuItems { LINKTYPE_TAGS == it.id }
        cache.resetLinkcacheFor(LINKTYPE_TAGS)

        // Add to menu
        internalImageInformation
            .forEachIndexed { idx, imageInformation ->
                imageInformation.tags.forEach { tagName ->
                    val tag = tagName
                    computedTags.computeIfAbsent(tag) { mutableSetOf() } += imageInformation
                    // Add tag URLs to global cache
                    "gallery/tags/${tag.url}".let { link ->
                        cache.addLinkcacheEntryFor(LINKTYPE_TAGS, tag.url, link)
                        if (shallAddToMenu)
                            cache.addMenuItemInContainerNoDupes(
                                LINKTYPE_TAGS,
                                "Tags",
                                300,
                                tag.name,
                                link,
                                orderFunction = { idx },
                                elementIndex = idx
                            )
                    }
                }
            }
    }

    private suspend fun buildImageInformation(configuration: WebsiteConfiguration) {
        configuration.inPath.withChild(AbstractThumbnailGenerator.NAME_IMAGE_SUBFOLDER)
            .list()
            .filter { it.fileExtension.isJpeg() }
            .asIterable()
            .forEachLimitedParallel(20) { jpegPath ->
                val filenameWithoutExtension = jpegPath.filenameWithoutExtension
                val configPath = jpegPath.parent withChild "$filenameWithoutExtension.json"
                val thumbnailInfoPath =
                    configuration.tmpPath withChild AbstractThumbnailGenerator.NAME_THUMBINFO_SUBFOLDER withChild "$filenameWithoutExtension.cache.json"
                if (!configPath.exists())
                    throw IllegalStateException("Config path does not exist for $jpegPath!")
                if (!jpegPath.exists())
                    throw IllegalStateException("Image path does not exist for $jpegPath!")
                if (!thumbnailInfoPath.exists())
                    throw IllegalStateException("Thumbnail Info path does not exist for $jpegPath!")


                val imageConfig = configPath.fromJson<ImageConfig>()
                    ?: throw IllegalStateException("Could not load config file $configPath. Please check if the format is valid!")
                val exifData = jpegPath.readExif(exifReplaceFunction)
                val thumbnailConfig: HashMap<AbstractThumbnailGenerator.ImageSize, AbstractThumbnailGenerator.ImageSizeInformation> =
                    thumbnailInfoPath.fromJson() ?: throw IllegalStateException("Thumbnail info not found...")

                val tags = imageConfig.tags
                    .map { TagInformation(it) }
                    .toCollection(concurrentSetOf())
                val imageInformation = InternalImageInformation(
                    filenameWithoutExtension,
                    thumbnailConfig,
                    SUBFOLDER_OUT + '/' + filenameWithoutExtension.lowercase(Locale.getDefault()),
                    imageConfig.title,
                    tags, // Needed because single element sets are not correctly loaded as MutableSet
                    exifData
                )

                cache.imageInformationData[filenameWithoutExtension] = imageInformation
                imageConfig.categoryThumbnail.forEach { category ->
                    val categoryName = CategoryName(category)
                    cache.computedCategoryThumbnails.let { thumbnails ->
                        thumbnails[categoryName].let {
                            if (it != null)
                                throw IllegalStateException("A thumbnail for $category has already been set! (\"${it.title}\"")
                            else
                                thumbnails[categoryName] = imageInformation
                        }
                    }
                }
            }
    }

    private suspend fun buildCategories(
        websiteConfiguration: WebsiteConfiguration,
        cache: BuildingCache
    ) = with(this.cache) {

        val images = imageInformationData.values.asSequence()
            .map { it as? InternalImageInformation }
            .filterNotNull()
            .toList()

        val computedCategories = categoryConfig.computeCategoryInformation(images, websiteConfiguration)
        categoryInformation = computedCategories

        val shallAddToMenu = displayedMenuItems.contains(DisplayedMenuItems.CATEGORIES)

        computedCategories.flatten()
            .sortedBy { it.categoryName.sortKey }
            .forEachIndexed { idx, category ->
                val link = "gallery/categories/${category.urlFragment}"

                cache.addLinkcacheEntryFor(LINKTYPE_CATEGORIES, category.categoryName.complexName, link)

                if (shallAddToMenu && category.visible) {
                    cache.addMenuItemInContainer(
                        LINKTYPE_CATEGORIES,
                        "Kategorien",
                        200,
                        category.categoryName.displayName,
                        link,
                        elementIndex = idx
                    )
                }
            }
    }

    private suspend fun generateWebpages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        testimonialLoader: TestimonialLoader,
    ) {
        generateImagePages(configuration, cache, testimonialLoader)
        generateCategoryPages(configuration, cache, testimonialLoader)
        generateTagPages(configuration, cache, testimonialLoader)
    }

    private suspend fun generateImagePages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        testimonialLoader: TestimonialLoader,
    ) {
        this.cache.imageInformationData.values
            .asSequence()
            .map { it as? InternalImageInformation }
            .filterNotNull()
            .asIterable()
            .forEachParallel { curImageInformation ->
                generateImagePage(configuration, cache, testimonialLoader, curImageInformation)
            }
    }

    protected abstract fun generateImagePage(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        testimonialLoader: TestimonialLoader,
        curImageInformation: InternalImageInformation
    )

    private suspend fun generateCategoryPages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        testimonialLoader: TestimonialLoader,
    ) {
        val categories = this.cache.categoryInformation?.flatten() ?: emptySequence()

        categories.asIterable().forEachParallel {
            generateCategoryPage(configuration, cache, testimonialLoader, it)
        }
    }

    protected abstract fun generateCategoryPage(
        configuration: WebsiteConfiguration,
        buildingCache: BuildingCache,
        testimonialLoader: TestimonialLoader,
        categoryInformation: CategoryInformation,
    )

    protected suspend fun generateTagPages(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        testimonialLoader: TestimonialLoader,
    ) {
        this.cache.computedTags.keys.forEachParallel { tagName ->
            generateTagPage(configuration, cache, testimonialLoader, tagName)
        }
    }

    protected abstract fun generateTagPage(
        configuration: WebsiteConfiguration,
        buildingCache: BuildingCache,
        testimonialLoader: TestimonialLoader,
        tagName: TagInformation
    )

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean = if (changeFiles.hasRelevantChanges()) {
        cleanup(configuration, cache)
        fetchInitialInformation(configuration, cache, alreadyRunGenerators)
        true
    } else false

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ) = if (changeFiles.hasRelevantChanges()) {
        buildInitialArtifacts(configuration, cache)
        true
    } else false

    private fun ChangeFileset.hasRelevantChanges() =
        keys.asSequence()
            .any { it.hasExtension(FileExtension::isJson) }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        withContext(Dispatchers.IO) {
            configuration.outPath.resolve(SUBFOLDER_OUT)
                .toFile()
                .deleteRecursively()
        }
    }

    fun Sequence<InternalImageInformation>.toOrderedByTime() =
        sortedByDescending { it.exifInformation[ExifdataKey.CREATION_DATETIME] }
            .toList()

    override suspend fun loadCache(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.loadCache(configuration, cache)
        cachePath = configuration.tmpPath withChild "gallery.cache.json"
    }

    override suspend fun saveCache(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.saveCache(configuration, cache)
        this.cache.toJson(cachePath)
    }

    class GalleryMinimalInfo(override val sourcePath: SourcePath, override val targetPath: TargetPath) :
        IPageMininmalInfo {
        override val title: String
            get() = throw IllegalStateException("Not Implemented")

    }

    protected fun DIV.insertCustomMarkdown(
        outFolder: Path,
        type: String,
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        testimonialLoader: TestimonialLoader
    ): HEAD.() -> Unit {
        val inPath =
            configuration.inPath withChild configuration.outPath.relativize(outFolder) withChild "$type.gallery.md"
        if (inPath.exists()) {
            val (_, manipulator, html) = MarkdownParser.parse(
                configuration,
                cache,
                testimonialLoader,
                GalleryMinimalInfo(inPath, outFolder withChild "index.html"),
                this@AbstractGalleryGenerator
            )
            raw(html)
            return manipulator
        } else
            return {}
    }
}

fun DIV.insertSubcategoryThumbnails(
    subcategories: Set<CategoryInformation>,
    configuration: WebsiteConfiguration
) {
    if (subcategories.isEmpty()) return
    insertCategoryThumbnails(subcategories, configuration)
}

fun DIV.insertCategoryThumbnails(
    categoriesToDisplay: Set<CategoryInformation>,
    configuration: WebsiteConfiguration
) {
    if (categoriesToDisplay.isNotEmpty())
        div("category-thumbnails") {
            categoriesToDisplay.asSequence()
                .filter { it.visible }
                .map {
                    it to it.thumbnailImage
                }
                .filter { (_, it) -> it != null }
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it as Pair<CategoryInformation, InternalImageInformation>
                }
                .sortedBy { (categoryInformation, _) -> categoryInformation.categoryName.sortKey }
                .forEach { (categoryName, imageInformation) ->
                    insertSubcategoryThumbnail(
                        categoryName,
                        imageInformation,
                        configuration
                    )
                }
        }
}

fun Map<FilenameWithoutExtension, ImageInformation>.getOrThrow(
    key: FilenameWithoutExtension,
    usage: TargetPath? = null
) =
    getOrThrow(key, usage?.filenameWithoutExtension)

fun Map<FilenameWithoutExtension, ImageInformation>.getOrThrow(key: FilenameWithoutExtension, usage: String? = null) =
    this[key] ?: throw IllegalStateException("Cannot find picture with filename \"$key\" (used in ${usage})!")

fun List<WebsiteGenerator>.findGalleryGenerator() =
    find { it is AbstractGalleryGenerator }
            as? AbstractGalleryGenerator
        ?: throw IllegalStateException("Gallery generator is needed for this generator!")
