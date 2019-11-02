package pictures.reisishot.mise.backend.generator.gallery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.DIV
import kotlinx.html.div
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.html.insertSubcategoryThumbnail
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap

abstract class AbstractGalleryGenerator(private vararg val categoryBuilders: CategoryBuilder,
                                        private val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }) : WebsiteGenerator, ImageInformationRepository {
    internal var cache = Cache()
    protected lateinit var cachePath: Path
    private val menuIemComperator = Comparator.comparing<MenuLinkContainerItem, String> { it.text }
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
        val newestFile = configuration.inPath.withChild(AbstractThumbnailGenerator.NAME_IMAGE_SUBFOLDER).list()
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

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) =
            generateWebpages(configuration, cache)

    protected fun buildTags(cache: BuildingCache) = with(this.cache) {
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

    // TODO Write cache files and use them instead of computing...
    private suspend fun buildImageInformation(configuration: WebsiteConfiguration) {
        (configuration.inPath withChild AbstractThumbnailGenerator.NAME_IMAGE_SUBFOLDER).list().filter { it.fileExtension.isJpeg() }
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
                    val thumbnailConfig: HashMap<AbstractThumbnailGenerator.ImageSize, AbstractThumbnailGenerator.ThumbnailInformation> =
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

    private suspend fun buildCategories(
            websiteConfiguration: WebsiteConfiguration,
            cache: BuildingCache
    ) = with(this.cache) {
        val categoryLevelMap: MutableMap<Int, MutableSet<CategoryInformation>> = ConcurrentHashMap()
        cache.clearMenuItems { LINKTYPE_CATEGORIES == it.id }
        cache.resetLinkcacheFor(LINKTYPE_CATEGORIES)

        categoryBuilders.forEach { categoryBuilder ->
            categoryBuilder.generateCategories(this@AbstractGalleryGenerator, websiteConfiguration)
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

    protected abstract suspend fun generateImagePages(configuration: WebsiteConfiguration, cache: BuildingCache)
    protected abstract fun generateCategoryPages(configuration: WebsiteConfiguration, cache: BuildingCache)
    protected abstract fun generateTagPages(configuration: WebsiteConfiguration, cache: BuildingCache)

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

    fun Collection<InternalImageInformation>.toOrderedByTimeArray() = asSequence().toOrderedByTimeArray(size)
    fun Sequence<InternalImageInformation>.toOrderedByTimeArray(size: Int) = sortedByDescending { it.exifInformation[ExifdataKey.CREATION_TIME] }
            .toArray(size)

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
}

fun Map<CategoryName, InternalImageInformation>.getThumbnailImageInformation(
        name: CategoryName,
        generator: AbstractGalleryGenerator
): InternalImageInformation? =
        get(name)
                ?: generator.cache.imageInformationData[generator.cache.computedCategories[name]?.first()]
                ?: throw IllegalStateException("Could not find thumbnail for \"$name\"!")

fun DIV.insertSubcategoryThumbnails(categoryName: CategoryName?, generator: AbstractGalleryGenerator) = with(generator.cache) {
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