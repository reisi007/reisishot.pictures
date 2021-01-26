package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.*
import at.reisishot.mise.config.ImageConfig
import at.reisishot.mise.config.parseConfig
import at.reisishot.mise.exifdata.ExifdataKey
import at.reisishot.mise.exifdata.readExif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.DIV
import kotlinx.html.HEAD
import kotlinx.html.div
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.fromXml
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.html.insertSubcategoryThumbnail
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import pictures.reisishot.mise.backend.htmlparsing.TargetPath
import pictures.reisishot.mise.backend.toXml
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.streams.asSequence


abstract class AbstractGalleryGenerator(
        private val categoryBuilders: Array<CategoryBuilder>,
        private val displayedMenuItems: Set<DisplayedMenuItems> = setOf(DisplayedMenuItems.CATEGORIES, DisplayedMenuItems.TAGS),
        private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : WebsiteGenerator, ImageInformationRepository {

    enum class DisplayedMenuItems {
        CATEGORIES,
        TAGS;
    }

    override val executionPriority: Int = 20_000

    internal var cache = Cache()
    private lateinit var cachePath: Path
    override val imageInformationData: Collection<ImageInformation> = cache.imageInformationData.values
    override val computedTags: Map<TagInformation, Set<ImageInformation>> = cache.computedTags
    protected val ExifdataKey.displayName
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

    data class Cache(
            val imageInformationData: MutableMap<FilenameWithoutExtension, ImageInformation> = concurrentSkipListMap(),
            val categoryInformation: MutableMap<CategoryName, CategoryInformation> = concurrentSkipListMap(),
            val computedTags: MutableMap<TagInformation, MutableSet<ImageInformation>> = concurrentSkipListMap(compareBy(TagInformation::name)),
            val computedCategories: MutableMap<CategoryName, MutableSet<FilenameWithoutExtension>> = concurrentSkipListMap(),
            val computedSubcategories: MutableMap<CategoryName, Set<CategoryName>> = concurrentSkipListMap(),
            val computedCategoryThumbnails: MutableMap<CategoryName, ImageInformation> = concurrentSkipListMap()
    ) {
        companion object {
            private fun <K, V> concurrentSkipListMap(comparator: Comparator<in K>): MutableMap<K, V> = ConcurrentSkipListMap(comparator)
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
    ) = buildCache(configuration, cache)

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
            this.cache = cachePath.fromXml()
                    ?: throw IllegalStateException("Cache has a modified date, but cannot be parsed!")
        }

        val regeneratePages = !cacheStillValid || hasTextChanges(configuration, cacheTime)

        if (!regeneratePages) return

        buildImageInformation(configuration)
        buildTags(cache)
        buildCategories(configuration, cache)
    }

    private suspend fun hasTextChanges(configuration: WebsiteConfiguration, cacheTime: ZonedDateTime) = withContext(Dispatchers.IO) {
        // A file has been modified, which is newer than the cache
        val path = configuration.inPath withChild "gallery"
        if (!path.exists())
            return@withContext false
        return@withContext Files.walk(path)
                .asSequence()
                .map { it.fileModifiedDateTime }
                .filterNotNull()
                .any { it >= cacheTime }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) =
            generateWebpages(configuration, cache)

    private fun buildTags(cache: BuildingCache) = with(this.cache) {
        val shallAddToMenu = displayedMenuItems.contains(DisplayedMenuItems.TAGS)
        cache.clearMenuItems { LINKTYPE_TAGS == it.id }
        cache.resetLinkcacheFor(LINKTYPE_TAGS)
        imageInformationData.values.asSequence()
                .map { it as? InternalImageInformation }
                .filterNotNull()
                .forEachIndexed { idx, imageInformation ->
                    imageInformation.tags.forEach { tagName ->
                        val tag = TagInformation(tagName)
                        computedTags.computeIfAbsent(tag) { mutableSetOf() } += imageInformation
                        // Add tag URLs to global cache
                        "gallery/tags/${tag.url}".let { link ->
                            cache.addLinkcacheEntryFor(LINKTYPE_TAGS, tag.name, link)
                            if (shallAddToMenu)
                                cache.addMenuItemInContainerNoDupes(LINKTYPE_TAGS, "Tags", 300, tag.name, link, orderFunction = { idx }, elementIndex = idx)
                        }
                    }
                }
    }

    // TODO Write cache files and use them instead of computing...
    private suspend fun buildImageInformation(configuration: WebsiteConfiguration) {
        configuration.inPath.withChild(AbstractThumbnailGenerator.NAME_IMAGE_SUBFOLDER)
                .list()
                .filter { it.fileExtension.isJpeg() }
                .asIterable()
                .forEachLimitedParallel(20) { jpegPath ->
                    val filenameWithoutExtension = jpegPath.filenameWithoutExtension
                    val configPath = jpegPath.parent withChild "$filenameWithoutExtension.conf"
                    val thumbnailInfoPath =
                            configuration.tmpPath withChild AbstractThumbnailGenerator.NAME_THUMBINFO_SUBFOLDER withChild "$filenameWithoutExtension.cache.xml"
                    if (!configPath.exists())
                        throw IllegalStateException("Config path does not exist for $jpegPath!")
                    if (!jpegPath.exists())
                        throw IllegalStateException("Image path does not exist for $jpegPath!")
                    if (!thumbnailInfoPath.exists())
                        throw IllegalStateException("Thumbnail Info path does not exist for $jpegPath!")


                    val imageConfig: ImageConfig = configPath.parseConfig()
                            ?: throw IllegalStateException("Could not load config file $configPath. Please check if the format is valid!")
                    val exifData = jpegPath.readExif(exifReplaceFunction)
                    val thumbnailConfig: HashMap<AbstractThumbnailGenerator.ImageSize, AbstractThumbnailGenerator.ImageSizeInformation> =
                            thumbnailInfoPath.fromXml() ?: throw IllegalStateException("Thumbnail info not found...")

                    val imageInformation = InternalImageInformation(
                            filenameWithoutExtension,
                            thumbnailConfig,
                            SUBFOLDER_OUT + '/' + filenameWithoutExtension.toLowerCase(),
                            imageConfig.title,
                            imageConfig.showInGallery,
                            imageConfig.tags,
                            exifData
                    )

                    cache.imageInformationData[filenameWithoutExtension] = imageInformation
                    imageConfig.categoryThumbnail.forEach { category ->
                        val categoryName = CategoryName(category)
                        cache.computedCategoryThumbnails.let { thumbnails ->
                            thumbnails.get(categoryName).let {
                                if (it != null)
                                    throw IllegalStateException("A thumbnail for $category has already been set! (\"${it.title}\"")
                                else
                                    thumbnails.put(categoryName, imageInformation)
                            }
                        }
                    }
                }
    }

    private suspend fun buildCategories(
            websiteConfiguration: WebsiteConfiguration,
            cache: BuildingCache
    ) {
        with(this.cache) {
            val shallAddToMenu = displayedMenuItems.contains(DisplayedMenuItems.CATEGORIES)

            val categoryLevelMap: MutableMap<Int, MutableSet<CategoryInformation>> = ConcurrentHashMap()
            cache.clearMenuItems { LINKTYPE_CATEGORIES == it.id }
            cache.resetLinkcacheFor(LINKTYPE_CATEGORIES)

            categoryBuilders.forEach { categoryBuilder ->
                buildCategory(categoryBuilder, websiteConfiguration, categoryLevelMap, cache, shallAddToMenu)
            }

            categoryLevelMap.values.asSequence()
                    .flatMap { it.asSequence() }
                    .map { it.internalName to it.subcategoryComputator(categoryLevelMap) }
                    .forEach { (category, subcategories) ->
                        computedSubcategories.put(category, subcategories)
                    }

            // Add first level subcategories
            categoryLevelMap[0]
                    ?.asSequence()
                    ?.filter { it.visible }
                    ?.map { it.internalName }
                    ?.filter { it.complexName.isNotBlank() }
                    ?.toSet()
                    ?.let { firstLevelCategories ->
                        computedSubcategories.put(CategoryName(""), firstLevelCategories)
                    }
        }
    }

    private suspend fun Cache.buildCategory(
            categoryBuilder: CategoryBuilder,
            websiteConfiguration: WebsiteConfiguration,
            categoryLevelMap: MutableMap<Int, MutableSet<CategoryInformation>>,
            cache: BuildingCache,
            shallAddToMenu: Boolean
    ) {
        categoryBuilder.generateCategories(this@AbstractGalleryGenerator, websiteConfiguration)
                .forEachIndexed { idx, (filename, categoryInformation) ->
                    categoryInformation.internalName.let { categoryName ->
                        categoryName.complexName.count { it == '/' }.let { subcategoryLevel ->
                            categoryLevelMap.computeIfAbsent(subcategoryLevel) { mutableSetOf() } += categoryInformation
                        }
                        computedCategories.computeIfAbsent(categoryInformation.internalName) {
                            val link = "gallery/categories/${categoryInformation.urlFragment}"
                            cache.addLinkcacheEntryFor(LINKTYPE_CATEGORIES, categoryInformation.complexName, link)
                            if (shallAddToMenu && categoryInformation.visible) {
                                cache.addMenuItemInContainer(
                                        LINKTYPE_CATEGORIES, "Kategorien", 200, categoryInformation.displayName,
                                        link, orderFunction = { idx }, elementIndex = idx
                                )
                            }
                            mutableSetOf()
                        } += filename

                        (imageInformationData[filename] as? InternalImageInformation)?.categories?.add(categoryInformation)
                        this.categoryInformation.computeIfAbsent(categoryInformation.internalName) { categoryInformation }
                    }
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
        this.cache.imageInformationData.values
                .asSequence()
                .map { it as? InternalImageInformation }
                .filterNotNull()
                .asIterable()
                .forEachLimitedParallel(50) { curImageInformation ->
                    generateImagePage(configuration, cache, curImageInformation)
                }
    }

    protected abstract fun generateImagePage(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            curImageInformation: InternalImageInformation
    )

    private fun generateCategoryPages(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    ) {
        this.cache.computedCategories.forEach { (categoryName, _) ->
            generateCategoryPage(configuration, cache, categoryName)
        }
    }

    protected abstract fun generateCategoryPage(
            configuration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            categoryName: CategoryName,
    )

    protected fun generateTagPages(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    ) {
        computedTags.keys.forEach { tagName ->
            generateTagPage(configuration, cache, tagName)
        }
    }

    protected abstract fun generateTagPage(
            configuration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            tagName: TagInformation
    )

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changeFiles: ChangeFileset): Boolean {
        if (changeFiles.hasRelevantChanges()) {
            cleanup(configuration, cache)
            fetchInitialInformation(configuration, cache, alreadyRunGenerators)
            return true
        } else return false
    }

    override suspend fun buildUpdateArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache, changeFiles: ChangeFileset): Boolean {
        if (changeFiles.hasRelevantChanges()) {
            buildInitialArtifacts(configuration, cache)
            return true
        } else return false
    }

    private fun ChangeFileset.hasRelevantChanges() =
            keys.asSequence()
                    .any { it.hasExtension(FileExtension::isConf) }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache): Unit {
        withContext(Dispatchers.IO) {
            configuration.outPath.resolve(SUBFOLDER_OUT)
                    .toFile()
                    .deleteRecursively()
        }
    }

    fun Sequence<InternalImageInformation>.toOrderedByTime() = sortedByDescending { it.exifInformation[ExifdataKey.CREATION_TIME] }
            .toList()

    override suspend fun loadCache(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.loadCache(configuration, cache)
        cachePath = configuration.tmpPath withChild "gallery.cache.xml"
        categoryBuilders.forEach { it.setup(configuration, cache) }
    }

    override suspend fun saveCache(configuration: WebsiteConfiguration, cache: BuildingCache) {
        super.saveCache(configuration, cache)
        this.cache.toXml(cachePath)
        categoryBuilders.forEach { it.teardown(configuration, cache) }
    }

    protected fun DIV.insertCustomMarkdown(outFolder: Path, type: String, configuration: WebsiteConfiguration, cache: BuildingCache): HEAD.() -> Unit {
        val inPath = configuration.inPath withChild configuration.outPath.relativize(outFolder) withChild "$type.gallery.md"
        if (inPath.exists()) {
            val (manipulator, html) = MarkdownParser.parse(configuration, cache, inPath, outFolder withChild "index.html", this@AbstractGalleryGenerator)
            raw(html)
            return manipulator
        } else
            return {}
    }
}

fun Map<CategoryName, ImageInformation>.getThumbnailImageInformation(
        name: CategoryName,
        generator: AbstractGalleryGenerator
): ImageInformation =
        get(name)
                ?: generator.cache.imageInformationData[generator.cache.computedCategories[name]?.first()]
                ?: throw IllegalStateException("Could not find thumbnail for \"$name\"!")

fun DIV.insertSubcategoryThumbnails(categoryName: CategoryName?, configuration: WebsiteConfiguration, generator: AbstractGalleryGenerator) {
    val subcategories = generator.cache.computedSubcategories[categoryName] ?: emptySet()
    if (subcategories.isNullOrEmpty()) return
    insertCategoryThumbnails(subcategories, configuration, generator)
}

fun DIV.insertCategoryThumbnails(subcategories: Set<CategoryName>, configuration: WebsiteConfiguration, generator: AbstractGalleryGenerator) {
    if (!subcategories.isNullOrEmpty())
        div("category-thumbnails") {
            subcategories.asSequence()
                    .map {
                        generator.cache.categoryInformation.getValue(it) to
                                generator.cache.computedCategoryThumbnails.getThumbnailImageInformation(it, generator)
                    }
                    .filterNotNull()
                    .sortedBy { (categoryInformation, _) -> categoryInformation.complexName }
                    .forEach { (categoryName, imageInformation) ->
                        insertSubcategoryThumbnail(
                                categoryName,
                                imageInformation,
                                configuration
                        )
                    }
        }
}

fun Map<FilenameWithoutExtension, ImageInformation>.getOrThrow(key: FilenameWithoutExtension, usage: TargetPath? = null) =
        getOrThrow(key, usage?.filenameWithoutExtension)

fun Map<FilenameWithoutExtension, ImageInformation>.getOrThrow(key: FilenameWithoutExtension, usage: String? = null) =
        this[key] ?: throw IllegalStateException("Cannot find picture with filename \"$key\" (used in ${usage})!")