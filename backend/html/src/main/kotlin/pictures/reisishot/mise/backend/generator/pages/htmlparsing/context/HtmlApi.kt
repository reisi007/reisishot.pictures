package pictures.reisishot.mise.backend.generator.pages.htmlparsing.context

import at.reisishot.mise.backend.config.BuildingCache
import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import at.reisishot.mise.commons.FilenameWithoutExtension
import kotlinx.html.*
import pictures.reisishot.mise.backend.ImageFetcher
import pictures.reisishot.mise.backend.ImageSize
import pictures.reisishot.mise.backend.html.*
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata
import kotlin.math.roundToInt

@WebsiteConfigBuilderDsl
fun createHtmlApi(
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    imageFetcher: ImageFetcher
): Pair<String, VelocityTemplateObjectCreator> =
    "html" to { pageMetadata, websiteConfig, cache ->
        HtmlApi(pageMetadata, cache, websiteConfig, imageSizes, fallback, imageFetcher)
    }

class HtmlApi(
    private val pageMetadata: PageMetadata?,
    private val cache: BuildingCache,
    private val websiteConfig: WebsiteConfig,
    private val imageSizes: Array<out ImageSize>,
    private val fallback: ImageSize,
    private val imageFetcher: ImageFetcher
) : TemplateObject {


    @SuppressWarnings("unused")
    @JvmOverloads
    fun insertPicture(filenameWithoutExtension: FilenameWithoutExtension, classNames: String? = null) = buildString {
        appendUnformattedHtml().div {
            insertLazyPicture(
                imageSizes,
                fallback,
                imageFetcher(filenameWithoutExtension),
                websiteConfig,
                "solo $classNames"
            )

        }
    }


    @SuppressWarnings("unused")
    fun insertGallery(
        galleryName: String,
        vararg filenameWithoutExtension: FilenameWithoutExtension
    ): String {
        return filenameWithoutExtension.asSequence()
            .map { imageFetcher(it) }
            .toList()
            .let { imageInformations ->
                buildString {
                    appendUnformattedHtml().div {
                        insertImageGallery(imageSizes, fallback, galleryName, websiteConfig, imageInformations)
                    }
                }

            }
    }

    @SuppressWarnings("unused")
    fun insertLink(type: String, key: String): String = cache.getLinkcacheEntryFor(websiteConfig, type, key)

    @SuppressWarnings("unused")
    fun insertLink(linktext: String, type: String, key: String): String = buildString {
        appendUnformattedHtml().a(insertLink(type, key)) {
            text(linktext)
        }
    }


    @SuppressWarnings("unused")
    @JvmOverloads
    fun toKontaktformular(text: String = "Zum Kontaktformular") = buildString {
        appendUnformattedHtml().div("center") {
            a("#footer", classes = "btn btn-primary") {
                text(text)
            }
        }
    }


    @SuppressWarnings("unused")
    fun addMeta(): String {
        val metadata = pageMetadata ?: throw IllegalStateException("No page metadata specified!")
        return buildString {
            appendUnformattedHtml().div {
                metadata(metadata)
            }
        }
    }

    @SuppressWarnings("unused")
    fun insertCarousel(changeMs: Int, vararg filename: FilenameWithoutExtension) =
        insertCarousel("carousel", changeMs, *filename)

    @SuppressWarnings("unused")
    fun insertCarousel(id: String, changeMs: Int, vararg imageFilenames: FilenameWithoutExtension) = buildString {
        appendUnformattedHtml().div {
            renderCarousel(imageSizes, fallback, id, changeMs, imageFilenames, imageFetcher, websiteConfig)
        }
    }

    @SuppressWarnings("unused")
    fun insertTextCarousel(changeMs: Int, vararg text: String) =
        insertTextCarousel("testimonials", changeMs, *text)

    @SuppressWarnings("unused")
    fun insertYoutube(codeOrLinkFragment: String, w: Int, h: Int) = buildString {
        appendUnformattedHtml().div {
            insertYoutube(codeOrLinkFragment, w, h)
        }
    }

    @SuppressWarnings("unused")
    fun insertTextCarousel(id: String, changeMs: Int, vararg text: String) = buildString {
        appendUnformattedHtml().div("carousel slide") {
            this.id = id
            attributes["data-bs-interval"] = changeMs.toString()
            attributes["data-bs-ride"] = "carousel"
            div("carousel-inner") {
                text.forEachIndexed { idx, cur ->
                    div {
                        classes = classes + "carousel-item"
                        if (idx == 0)
                            classes = classes + "active"

                        p {
                            span {
                                text("„$cur“")
                            }
                        }
                    }
                }
            }

            a("#$id", classes = "carousel-control-prev") {
                role = "button"
                attributes["data-bs-slide"] = "prev"
                span("carousel-control-prev-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("visually-hidden") { text("Vorheriger Text") }
            }

            a("#$id", classes = "carousel-control-next") {
                role = "button"
                attributes["data-bs-slide"] = "next"
                span("carousel-control-next-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("visually-hidden") { text("Nächster Text") }
            }
        }
    }

    @SuppressWarnings("unused")
    fun insertSlidingImages(filename: String, w: Int, h: Int) = buildString {
        fun getJpgUrl(filename: String, size: Int) = "https://images.reisishot.pictures/${filename}_${size}.jpg"
        fun getWebPUrl(filename: String, size: Int) = "https://images.reisishot.pictures/${filename}_${size}.webp"
        fun DIV.insertPictureFromImagesSubDomain(filename: String, alt: String, ratio: Float) {
            div(classes = PageGenerator.LAZYLOADER_CLASSNAME) {
                attributes["data-alt"] = alt
                sequenceOf(300, 400, 700, 1200, 2050, 3000).forEachIndexed { idx, size ->
                    val iw = if (ratio < 1) size else (size * ratio).roundToInt()
                    val ih = if (ratio > 1) size else (size * ratio).roundToInt()
                    attributes["data-$idx"] = """{"jpg":"${getJpgUrl(filename, size)}","webp":"${
                        getWebPUrl(
                            filename,
                            size
                        )
                    }","w":$iw,"h":$ih}"""
                }
                attributes["data-sizes"] = imageSizes.size.toString()
            }
        }


        appendUnformattedHtml().div("bal-container") {
            val ratio = (h / w.toFloat())
            attributes["style"] = "width: 550px;height:${Math.round(550 * ratio)}px"
            attributes["data-ratio"] = ratio.toString()
            div("bal-after") {
                insertPictureFromImagesSubDomain(filename + 'b', "Bearbeitet", ratio)
                div("bal-afterPosition afterLabel") {
                    text("Bearbeitet")
                }
            }
            div("bal-before") {
                div("bal-before-inset") {
                    insertPictureFromImagesSubDomain(filename + 'o', "Original", ratio)
                    div("bal-beforePosition beforeLabel") {
                        text("Original")
                    }
                }
            }
            div("bal-handle") {
                span("handle-left-arrow")
                span("handle-right-arrow")
            }
        }
    }

    @SuppressWarnings("unused")
    fun insertWartelisteInfo(): String = buildString {
        appendUnformattedHtml().div {
            insertWartelisteInfo()
        }
    }
}
