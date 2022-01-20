package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.backend.config.*
import at.reisishot.mise.commons.*
import at.reisishot.mise.config.ImageConfig
import at.reisishot.mise.exifdata.ExifdataKey
import at.reisishot.mise.exifdata.readExif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.*
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.SourcePath
import pictures.reisishot.mise.backend.TargetPath
import pictures.reisishot.mise.backend.config.category.*
import pictures.reisishot.mise.backend.config.tags.TagConfig
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.backend.generator.gallery.context.insertLazyPicture
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.thumbnail.ImageSizeInformation
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentMap
import kotlin.io.path.exists
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

    lateinit var cache: Cache
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
    class Cache(
        val imageInformationData: MutableMap<FilenameWithoutExtension, ImageInformation>,
        val computedTags: MutableMap<TagInformation, out Set<ImageInformation>>,
        val rootCategory: CategoryInformationRoot,

        ) {
        val subcategoryMap: Map<String, CategoryInformation> by lazy {
            rootCategory.flatten()
                .map { it.categoryName.complexName to it }
                .toMap()
        }
    }

    companion object {
        const val LINKTYPE_TAGS = "TAGS"
        const val LINKTYPE_CATEGORIES = "CATEGORIES"
        const val SUBFOLDER_OUT = "gallery/images"
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        buildCache(configuration, cache)
    }

    private suspend fun buildCache(
        configuration: WebsiteConfig,
        cache: BuildingCache
    ) = configuration.useJsonParserParallel {
        val cacheTime = cachePath.fileModifiedDateTime
            ?: kotlin.run { ZonedDateTime.of(LocalDate.of(1900, 1, 1), LocalTime.MIN, ZoneId.systemDefault()) }

        val cacheStillValid = cachePath.exists() &&
                configuration.paths.sourceFolder.withChild(AbstractThumbnailGenerator.NAME_IMAGE_SUBFOLDER)
                    .list()
                    .map { it.fileModifiedDateTime }
                    .filterNotNull()
                    .all { it < cacheTime }

        if (cacheStillValid) {
            this@AbstractGalleryGenerator.cache = cachePath.fromJson()
                ?: throw IllegalStateException("Cache has a modified date, but cannot be parsed!")
        }

        val recomputeGallery =
            !cacheStillValid
                    || hasTextChanges(configuration, cacheTime)
                    || hasConfigChanges(configuration, cacheTime)

        if (!recomputeGallery) return@useJsonParserParallel

        val imageInformation = buildImageInformation(configuration)
        val tagInformation = buildTags(imageInformation, cache)
        val categories = buildCategories(configuration, imageInformation, cache)

        this@AbstractGalleryGenerator.cache = Cache(imageInformation, tagInformation, categories)
    }

    private fun hasConfigChanges(configuration: WebsiteConfig, cacheTime: ZonedDateTime): Boolean {
        return sequenceOf(
            "categories.json",
            "tags.json"
        ).map { configuration.paths.sourceFolder withChild it }
            .map { it.fileModifiedDateTime }
            .filterNotNull()
            .any { it >= cacheTime }
    }

    private fun hasTextChanges(configuration: WebsiteConfig, cacheTime: ZonedDateTime): Boolean {
        // A file has been modified, which is newer than the cache
        val path = configuration.paths.sourceFolder withChild "gallery"
        if (!path.exists())
            return false
        return Files.walk(path)
            .asSequence()
            .map { it.fileModifiedDateTime }
            .filterNotNull()
            .any { it >= cacheTime }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, cache: BuildingCache) =
        generateWebpages(configuration, cache)

    private fun buildTags(
        imageInformationData: ConcurrentMap<FilenameWithoutExtension, ImageInformation>,
        cache: BuildingCache
    ): ConcurrentMap<TagInformation, out Set<ImageInformation>> {
        val internalImageInformation = imageInformationData.values.asSequence()
            .map { it as? InternalImageInformation }
            .filterNotNull()
            .toList()
        tagConfig.computeTags(internalImageInformation)

        val shallAddToMenu = displayedMenuItems.contains(DisplayedMenuItems.TAGS)
        cache.clearMenuItems { LINKTYPE_TAGS == it.id }
        cache.resetLinkcacheFor(LINKTYPE_TAGS)


        // Add to menu
        val computedTags = concurrentSkipListMap<TagInformation, MutableSet<ImageInformation>>(
            compareBy(
                TagInformation::name
            )
        )

        internalImageInformation.forEachIndexed { idx, imageInformation ->
            imageInformation.tags.forEach { tag ->
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
        return computedTags
    }

    private suspend fun buildImageInformation(configuration: WebsiteConfig) = configuration.useJsonParserParallel {
        val imageInformationData = concurrentSkipListMap<FilenameWithoutExtension, ImageInformation>()

        configuration.paths.sourceFolder.withChild(AbstractThumbnailGenerator.NAME_IMAGE_SUBFOLDER)
            .list()
            .filter { it.fileExtension.isJpeg() }
            .asIterable()
            .forEachParallel { jpegPath ->
                val filenameWithoutExtension = jpegPath.filenameWithoutExtension
                val configPath = jpegPath.parent withChild "$filenameWithoutExtension.json"
                val thumbnailInfoPath =
                    configuration.paths.cacheFolder withChild AbstractThumbnailGenerator.NAME_THUMBINFO_SUBFOLDER withChild "$filenameWithoutExtension.cache.json"
                if (!configPath.exists())
                    throw IllegalStateException("Config path does not exist for $jpegPath!")
                if (!jpegPath.exists())
                    throw IllegalStateException("Image path does not exist for $jpegPath!")
                if (!thumbnailInfoPath.exists())
                    throw IllegalStateException("Thumbnail Info path does not exist for $jpegPath!")


                val imageConfig = configPath.fromJson<ImageConfig>()
                    ?: throw IllegalStateException("Could not load config file $configPath. Please check if the format is valid!")
                val exifData = jpegPath.readExif(exifReplaceFunction)
                val thumbnailConfig: HashMap<ImageSize, ImageSizeInformation> =
                    thumbnailInfoPath.fromJson() ?: throw IllegalStateException("Thumbnail info not found...")

                val tags = imageConfig.tags
                    .map { TagInformation(it) }
                    .toCollection(concurrentSetOf())
                val imageInformation = InternalImageInformation(
                    filenameWithoutExtension,
                    thumbnailConfig.toMap(),
                    SUBFOLDER_OUT + '/' + filenameWithoutExtension.lowercase(Locale.getDefault()),
                    imageConfig.title,
                    tags, // Needed because single element sets are not correctly loaded as MutableSet
                    exifData
                )

                imageInformationData[filenameWithoutExtension] = imageInformation
            }
        imageInformationData
    }

    private fun buildCategories(
        websiteConfig: WebsiteConfig,
        imageInformationData: ConcurrentMap<FilenameWithoutExtension, ImageInformation>,
        cache: BuildingCache
    ): CategoryInformationRoot {

        val images = imageInformationData.values.asSequence()
            .map { it as? InternalImageInformation }
            .filterNotNull()
            .toList()

        val rootCategory = categoryConfig.computeCategoryInformation(images, websiteConfig.websiteInformation)

        val shallAddToMenu = displayedMenuItems.contains(DisplayedMenuItems.CATEGORIES)

        rootCategory.flatten()
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
        return rootCategory
    }

    private suspend fun generateWebpages(
        configuration: WebsiteConfig,
        cache: BuildingCache,
    ) {
        generateImagePages(configuration, cache)
        generateCategoryPages(configuration, cache)
        generateTagPages(configuration, cache)
    }

    private suspend fun generateImagePages(
        configuration: WebsiteConfig,
        cache: BuildingCache,
    ) {
        this.cache.imageInformationData.values
            .asSequence()
            .map { it as? InternalImageInformation }
            .filterNotNull()
            .asIterable()
            .forEachParallel { curImageInformation ->
                generateImagePage(configuration, cache, curImageInformation)
            }
    }

    protected abstract fun generateImagePage(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        curImageInformation: InternalImageInformation
    )

    private suspend fun generateCategoryPages(
        configuration: WebsiteConfig,
        cache: BuildingCache,
    ) {
        val categories = this.cache.rootCategory.flatten()

        categories.asIterable().forEachParallel {
            generateCategoryPage(configuration, cache, it)
        }
    }

    protected abstract fun generateCategoryPage(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        categoryInformation: CategoryInformation,
    )

    private suspend fun generateTagPages(
        configuration: WebsiteConfig,
        cache: BuildingCache,
    ) {
        this.cache.computedTags.keys.forEachParallel { tagName ->
            generateTagPage(configuration, cache, tagName)
        }
    }

    protected abstract fun generateTagPage(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        tagName: TagInformation
    )

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean = if (changeFiles.hasRelevantChanges()) {
        cleanup(configuration, cache)
        fetchInitialInformation(configuration, cache, alreadyRunGenerators)
        true
    } else false

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ) = if (changeFiles.hasRelevantChanges()) {
        buildInitialArtifacts(configuration, cache)
        true
    } else false

    private fun ChangeFileset.hasRelevantChanges() = keys.any { it.hasExtension(FileExtension::isJson) }

    override suspend fun cleanup(configuration: WebsiteConfig, cache: BuildingCache) {
        withContext(Dispatchers.IO) {
            configuration.paths.targetFolder.resolve(SUBFOLDER_OUT)
                .toFile()
                .deleteRecursively()
        }
    }

    fun Sequence<InternalImageInformation>.toOrderedByTime() =
        sortedByDescending { it.exifInformation[ExifdataKey.CREATION_DATETIME] }
            .toList()

    override suspend fun loadCache(configuration: WebsiteConfig, cache: BuildingCache) {
        super.loadCache(configuration, cache)
        cachePath = configuration.paths.cacheFolder withChild "gallery.cache.json"
    }

    override suspend fun saveCache(configuration: WebsiteConfig, cache: BuildingCache) =
        configuration.useJsonParserParallel {
            super.saveCache(configuration, cache)
            this@AbstractGalleryGenerator.cache.toJson(cachePath)
        }

    class GalleryMinimalInfo(override val sourcePath: SourcePath, override val targetPath: TargetPath) :
        IPageMinimalInfo {
        override val title: String
            get() = throw IllegalStateException("Not Implemented")

    }

    protected fun DIV.insertCustomMarkdown(
        outFolder: Path,
        type: String,
        configuration: WebsiteConfig,
        cache: BuildingCache,
    ): HEAD.() -> Unit {
        val inPath =
            configuration.paths.sourceFolder withChild configuration.paths.targetFolder.relativize(outFolder) withChild "$type.gallery.md"
        if (inPath.exists()) {
            val (_, manipulator, html) = MarkdownParser.parse(
                configuration,
                cache,
                GalleryMinimalInfo(inPath, outFolder withChild "index.html")
            )
            raw(html)
            return manipulator
        } else
            return {}
    }
}

fun DIV.insertSubcategoryThumbnails(
    galleryGenerator: AbstractGalleryGenerator,
    subcategories: Set<CategoryInformation>,
    configuration: WebsiteConfig
) {
    if (subcategories.isEmpty()) return
    insertCategoryThumbnails(galleryGenerator, subcategories, configuration)
}

fun DIV.insertCategoryThumbnails(
    galleryGenerator: AbstractGalleryGenerator,
    categoriesToDisplay: Set<CategoryInformation>,
    configuration: WebsiteConfig
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
                    a("/gallery/categories/${categoryName.urlFragment}", classes = "card") {
                        div("card-img-top") {
                            insertLazyPicture(
                                imageInformation,
                                configuration
                            )
                        }
                        div("card-body") {
                            h4("card-title") {
                                text("${categoryName.categoryName.displayName} (${categoryName.images.size})")
                            }
                        }
                    }
                }
        }
}
