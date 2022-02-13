package pictures.reisishot.mise.backend.gallery.generator

import kotlinx.html.DIV
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h4
import kotlinx.html.i
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.ChangeFileset
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteGenerator
import pictures.reisishot.mise.backend.config.category.CategoryConfigRoot
import pictures.reisishot.mise.backend.config.category.CategoryInformation
import pictures.reisishot.mise.backend.config.category.buildCategoryConfig
import pictures.reisishot.mise.backend.config.category.flatten
import pictures.reisishot.mise.backend.config.tags.TagConfig
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.backend.config.tags.buildTagConfig
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.context.insertImageGallery
import pictures.reisishot.mise.backend.generator.gallery.insertSubcategoryThumbnails
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.smallButtonLink
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.withChild
import pictures.reisishot.mise.config.ImageConfig
import pictures.reisishot.mise.exifdata.ExifdataKey
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class GalleryGenerator(
    tagConfig: TagConfig = buildTagConfig { },
    categoryConfig: CategoryConfigRoot = buildCategoryConfig { },
    displayedMenuItems: Set<DisplayedMenuItems> = setOf(DisplayedMenuItems.CATEGORIES, DisplayedMenuItems.TAGS),
    exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it },
    pageGenerationSettings: Set<PageGenerationSettings> = setOf(*PageGenerationSettings.values()),
    imageConfigNotFoundAction: (notFoundConfigPath: Path) -> ImageConfig = { inPath ->
        throw IllegalStateException(
            "Could not load config file $inPath. Please check if the format is valid!"
        )
    }
) : AbstractGalleryGenerator(
    tagConfig,
    categoryConfig,
    displayedMenuItems,
    pageGenerationSettings,
    exifReplaceFunction,
    imageConfigNotFoundAction
) {

    override val generatorName: String = "Reisishot Gallery"

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("eeee 'den' dd.MM.YYYY 'um' HH:mm:ss z")

    override fun generateImagePage(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        curImageInformation: ImageInformation
    ) {
        if (!(curImageInformation is InternalImageInformation)) return

        val baseHtmlPath = configuration.paths.targetFolder withChild "gallery/images"
        val targetFolder = baseHtmlPath withChild curImageInformation.filename.lowercase()
        PageGenerator.generatePage(
            websiteConfig = configuration,
            buildingCache = buildingCache,
            target = targetFolder withChild "index.html",
            title = curImageInformation.title
        ) {
            div("singleImage") {
                h1("text-center") {
                    text(curImageInformation.title)
                }

                insertCustomMarkdown(targetFolder, "start", configuration, buildingCache)

                insertImageGallery("1", configuration, curImageInformation)

                insertCategoryLinks(curImageInformation, configuration, buildingCache)

                insertTagLinks(curImageInformation, configuration, buildingCache)

                insertCustomMarkdown(targetFolder, "beforeExif", configuration, buildingCache)

                insertExifInformation(curImageInformation, dateTimeFormatter)

                insertCustomMarkdown(targetFolder, "end", configuration, buildingCache)
            }
        }
    }

    override fun generateCategoryPage(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        categoryInformation: CategoryInformation,
    ) {
        val categoryImages: List<FilenameWithoutExtension> = categoryInformation.images.asSequence()
            .map { it as? InternalImageInformation }
            .filterNotNull()
            .map { it.filename }
            .toList()

        (configuration.paths.targetFolder withChild "gallery/categories").let { baseHtmlPath ->

            val targetFolder = baseHtmlPath withChild categoryInformation.urlFragment
            PageGenerator.generatePage(
                websiteConfig = configuration,
                buildingCache = buildingCache,
                target = targetFolder withChild "index.html",
                title = categoryInformation.categoryName.displayName,
                pageContent = {
                    h1("text-center") {
                        text("Kategorie - ")
                        i {
                            text(("\"${categoryInformation.categoryName.displayName}\""))
                        }
                    }

                    insertCustomMarkdown(targetFolder, "start", configuration, buildingCache)

                    val imageInformations = with(categoryImages) {
                        asSequence()
                            .map { cache.imageInformationData.getValue(it) }
                            .map { it as? InternalImageInformation }
                            .filterNotNull()
                            .toOrderedByTime()
                    }

                    insertSubcategoryThumbnails(
                        categoryInformation.subcategories,
                        configuration
                    )

                    insertImageGallery("1", configuration, imageInformations)

                    insertCustomMarkdown(targetFolder, "end", configuration, buildingCache)
                }
            )
        }
    }

    override fun generateTagPage(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        tagInformation: TagInformation
    ): Unit = with(cache) {
        val tagImages = computedTags.getValue(tagInformation)
        val baseHtmlPath = configuration.paths.targetFolder withChild "gallery/tags"
        val targetFolder = baseHtmlPath withChild tagInformation.urlFragment
        PageGenerator.generatePage(
            websiteConfig = configuration,
            buildingCache = buildingCache,
            target = targetFolder withChild "index.html",
            title = tagInformation.name,
            pageContent = {
                h1("text-center") {
                    text("Tag - ")
                    i {
                        text(("\"${tagInformation.name}\""))
                    }
                }

                insertCustomMarkdown(targetFolder, "start", configuration, buildingCache)

                val imageInformations = tagImages.asSequence()
                    .map { it as? InternalImageInformation }
                    .filterNotNull()
                    .toOrderedByTime()
                insertImageGallery("1", configuration, imageInformations)

                insertCustomMarkdown(targetFolder, "end", configuration, buildingCache)
            }
        )
    }

    private fun DIV.insertTagLinks(
        curImageInformation: InternalImageInformation,
        configuration: WebsiteConfig,
        cache: BuildingCache
    ) {
        div("card") {
            h4("card-title") {
                text("Tags")
            }
            div("card-body btn-flex") {
                curImageInformation.tags.forEach { tagInformation ->
                    smallButtonLink(
                        tagInformation.name,
                        cache.getLinkcacheEntryFor(configuration, LINKTYPE_TAGS, tagInformation.urlFragment)
                    )
                }
            }
        }
    }

    private fun DIV.insertCategoryLinks(
        curImageInformation: InternalImageInformation,
        configuration: WebsiteConfig,
        cache: BuildingCache
    ) {
        div("card") {
            h4("card-title") {
                text("Kategorien")
            }
            div("card-body btn-flex") {
                curImageInformation.categories.asSequence()
                    .map { this@GalleryGenerator.cache.subcategoryMap[it]?.categoryName }
                    .filterNotNull()
                    .forEach { category ->
                        smallButtonLink(
                            category.displayName,
                            cache.getLinkcacheEntryFor(
                                configuration,
                                LINKTYPE_CATEGORIES,
                                category.complexName.lowercase()
                            )
                        )
                    }
            }
        }
    }

    private fun DIV.insertExifInformation(
        curImageInformation: InternalImageInformation,
        dateTimeFormatter: DateTimeFormatter
    ) {
        div("card") {
            h4("card-title") {
                text("Exif Informationen")
            }
            div("card-body") {
                div("card-text") {
                    curImageInformation.exifInformation.forEach { (type, value) ->
                        div("row justify-content-between") {
                            div("col-md-3 align-self-center") {
                                text("${type.displayName}:")
                            }
                            div("col-md-9 align-self-center") {
                                when (type) {
                                    ExifdataKey.CREATION_DATETIME -> text(
                                        ZonedDateTime.parse(value)
                                            .format(dateTimeFormatter)
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

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        // find files & compute changes on the go
        val filesToProcess = changeFiles.asSequence()
            .map { it.key }
            .map { configuration.paths.sourceFolder.relativize(it) }
            .filter { it.getName(0).toString() == "gallery" }
            .toList()
        val curUpdate = filesToProcess.isNotEmpty()
        updatePages(filesToProcess, configuration, buildingCache)
        // Keep super call. Extra variable -> should be executed in any case
        val superUpdate =
            super.fetchUpdateInformation(configuration, buildingCache, alreadyRunGenerators, changeFiles)
        return superUpdate || curUpdate
    }

    private fun updatePages(
        filesToProcess: List<Path>,
        configuration: WebsiteConfig,
        cache: BuildingCache
    ) {
        filesToProcess.forEach { updatePage(it, configuration, cache) }
    }

    private fun updatePage(
        path: Path,
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
    ): Unit = with(cache) {
        val type = path.getName(1).toString()
        val value = path.subpath(2, path.nameCount - 1)
            .toString()
            .replace('\\', '/')
        when (type) {
            "categories" -> {
                val categoryInformation = rootCategory.flatten()
                    .first { it.urlFragment.equals(value, true) }
                generateCategoryPage(configuration, buildingCache, categoryInformation)
            }
            "tags" -> {
                val tagName = computedTags
                    .keys
                    .first { it.name.equals(value, true) }
                generateTagPage(configuration, buildingCache, tagName)
            }
            "images" -> {
                val imageInformation = imageInformationData
                    .keys
                    .first { it.equals(value, true) }
                    .let { imageInformationData.getValue(it) }
                (imageInformation as? InternalImageInformation)?.let { internalImageInformation ->
                    generateImagePage(configuration, buildingCache, internalImageInformation)
                }
            }
            else -> throw IllegalStateException("Type $type is not known")
        }
    }

    fun Sequence<InternalImageInformation>.toOrderedByTime() =
        sortedByDescending { it.exifInformation[ExifdataKey.CREATION_DATETIME] }
            .toList()
}
