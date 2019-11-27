package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.forEachLimitedParallel
import at.reisishot.mise.commons.withChild
import kotlinx.html.*
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.smallButtonLink
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class GalleryGenerator(
        vararg val categoryBuilders: CategoryBuilder,
        val exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }
) : AbstractGalleryGenerator(*categoryBuilders, exifReplaceFunction = exifReplaceFunction) {

    override val generatorName: String = "Reisishot Gallery"
    override val executionPriority: Int = 20_000

    override suspend fun generateImagePages(
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


    override fun generateCategoryPages(
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


    override fun generateTagPages(
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
                                div("col-md-3 align-self-center") {
                                    text("${type.displayName}:")
                                }
                                div("col-md-9 align-self-center") {
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

    fun DIV.insertSubcategoryThumbnails(categoryName: CategoryName?) =
            insertSubcategoryThumbnails(categoryName, this@GalleryGenerator)
}
