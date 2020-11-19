package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.commons.forEachLimitedParallel
import at.reisishot.mise.commons.withChild
import at.reisishot.mise.exifdata.ExifdataKey
import kotlinx.html.*
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.smallButtonLink
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class GalleryGenerator(
        vararg val categoryBuilders: CategoryBuilder,
        displayedMenuItems: Set<DisplayedMenuItems> = setOf(DisplayedMenuItems.CATEGORIES, DisplayedMenuItems.TAGS),
        exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : AbstractGalleryGenerator(*categoryBuilders, displayedMenuItems = displayedMenuItems, exifReplaceFunction = exifReplaceFunction) {

    override val generatorName: String = "Reisishot Gallery"

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("eeee 'den' dd.MM.YYYY 'um' HH:mm:ss z")

    override suspend fun generateImagePages(
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

    private fun generateImagePage(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            curImageInformation: InternalImageInformation
    ) {
        val baseHtmlPath = configuration.outPath withChild "gallery/images"
        val targetFolder = baseHtmlPath withChild curImageInformation.filename.toLowerCase()
        PageGenerator.generatePage(
                websiteConfiguration = configuration,
                buildingCache = cache,
                target = targetFolder withChild "index.html",
                title = curImageInformation.title,
                pageContent = {
                    div("singleImage") {
                        h1("text-center") {
                            text(curImageInformation.title)
                        }

                        insertCustomMarkdown(targetFolder, "start", configuration, cache)

                        insertImageGallery("1", curImageInformation)

                        insertCategoryLinks(curImageInformation, configuration, cache)

                        insertTagLinks(curImageInformation, configuration, cache)

                        insertCustomMarkdown(targetFolder, "beforeExif", configuration, cache)

                        insertExifInformation(curImageInformation, dateTimeFormatter)

                        insertCustomMarkdown(targetFolder, "end", configuration, cache)
                    }
                }
        )
    }


    override fun generateCategoryPages(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    ) {
        this.cache.computedCategories.forEach { (categoryName, _) ->
            generateCategoryPage(configuration, cache, categoryName)
        }
    }


    private fun generateCategoryPage(
            configuration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            categoryName: CategoryName,
    ) {
        val categoryImages: Set<FilenameWithoutExtension> = cache.computedCategories.getValue(categoryName)
        (configuration.outPath withChild "gallery/categories").let { baseHtmlPath ->

            val categoryMetaInformation = cache.categoryInformation[categoryName]
                    ?: throw IllegalStateException("No category information found for name \"$categoryName\"!")
            val targetFolder = baseHtmlPath withChild categoryMetaInformation.urlFragment
            PageGenerator.generatePage(
                    websiteConfiguration = configuration,
                    buildingCache = buildingCache,
                    target = targetFolder withChild "index.html",
                    title = categoryMetaInformation.displayName,
                    pageContent = {
                        h1("text-center") {
                            text("Kategorie - ")
                            i {
                                text(("\"${categoryMetaInformation.displayName}\""))
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

                        insertSubcategoryThumbnails(categoryMetaInformation.internalName)

                        insertImageGallery("1", imageInformations)

                        insertCustomMarkdown(targetFolder, "end", configuration, buildingCache)
                    }
            )
        }

    }


    override fun generateTagPages(
            configuration: WebsiteConfiguration,
            cache: BuildingCache
    ) {
        computedTags.keys.forEach { tagName ->
            generateTagPage(configuration, cache, tagName)
        }
    }

    private fun generateTagPage(
            configuration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            tagName: TagInformation
    ) {
        val tagImages = computedTags.getValue(tagName)
        val baseHtmlPath = configuration.outPath withChild "gallery/tags"
        val targetFolder = baseHtmlPath withChild tagName.url
        PageGenerator.generatePage(
                websiteConfiguration = configuration,
                buildingCache = buildingCache,
                target = targetFolder withChild "index.html",
                title = tagName.name,
                pageContent = {
                    h1("text-center") {
                        text("Tag - ")
                        i {
                            text(("\"${tagName.name}\""))
                        }
                    }

                    insertCustomMarkdown(targetFolder, "start", configuration, buildingCache)

                    val imageInformations = tagImages.asSequence()
                            .map { it as? InternalImageInformation }
                            .filterNotNull()
                            .toOrderedByTime()
                    insertImageGallery("1", imageInformations)

                    insertCustomMarkdown(targetFolder, "end", configuration, buildingCache)
                })
    }

    private fun DIV.insertTagLinks(curImageInformation: InternalImageInformation, configuration: WebsiteConfiguration, cache: BuildingCache) {
        div("card") {
            h4("card-title") {
                text("Tags")
            }
            div("card-body btn-flex") {
                curImageInformation.tags.forEach { category ->
                    smallButtonLink(category, cache.getLinkcacheEntryFor(configuration, LINKTYPE_TAGS, category))
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
                    smallButtonLink(category.displayName, cache.getLinkcacheEntryFor(configuration, LINKTYPE_CATEGORIES, category.complexName))
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
                                div("col-md-3 align-self-center") {
                                    text("${type.displayName}:")
                                }
                                div("col-md-9 align-self-center") {
                                    when (type) {
                                        ExifdataKey.CREATION_TIME -> text(
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
    }

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changeFiles: ChangeFileset): Boolean {
        // find files & compute changes on the go
        val filesToProcess = changeFiles.asSequence()
                .map { it.key }
                .map { configuration.inPath.relativize(it) }
                .filter { it.getName(0).toString() == "gallery" }
                .toList()
        val curUpdate = filesToProcess.isNotEmpty()
        updatePages(filesToProcess, configuration, cache)
        // Keep super call. Extra variable -> should be executed in any case
        val superUpdate = super.fetchUpdateInformation(configuration, cache, alreadyRunGenerators, changeFiles)
        return superUpdate || curUpdate
    }

    private fun updatePages(filesToProcess: List<Path>, configuration: WebsiteConfiguration, cache: BuildingCache) {
        filesToProcess.forEach { updatePage(it, configuration, cache) }
    }

    private fun updatePage(path: Path, configuration: WebsiteConfiguration, buildingCache: BuildingCache) {
        val type = path.getName(1).toString()
        val value = path.subpath(2, path.nameCount - 1)
                .toString()
                .replace('\\', '/')
        when (type) {
            "categories" -> {
                val categoryName = cache.categoryInformation
                        .keys
                        .first { it.complexName.equals(value, true) }
                generateCategoryPage(configuration, buildingCache, categoryName)
            }
            "tags" -> {
                val tagName = computedTags
                        .keys
                        .first { it.name.equals(value, true) }
                generateTagPage(configuration, buildingCache, tagName)
            }
            "images" -> {
                val imageInformation = cache.imageInformationData
                        .keys
                        .first { it.equals(value, true) }
                        .let { cache.imageInformationData.getValue(it) }
                (imageInformation as? InternalImageInformation)?.let { internalImageInformation ->
                    generateImagePage(configuration, buildingCache, internalImageInformation)
                }

            }
            else -> throw IllegalStateException("Type $type is not known")
        }
    }

    fun DIV.insertSubcategoryThumbnails(categoryName: CategoryName?) =
            insertSubcategoryThumbnails(categoryName, this@GalleryGenerator)


}
