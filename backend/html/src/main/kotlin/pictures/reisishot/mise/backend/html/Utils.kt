package pictures.reisishot.mise.backend.html

import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.commons.FilenameWithoutExtension
import kotlinx.html.*
import kotlinx.html.impl.DelegatingMap
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.ImageFetcher
import pictures.reisishot.mise.backend.ImageInformation
import pictures.reisishot.mise.backend.ImageSize
import pictures.reisishot.mise.backend.df_dd_MM_YYYY
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata
import java.util.*


@HtmlTagMarker
fun HTMLTag.raw(block: StringBuilder.() -> Unit): Unit = consumer.onTagContentUnsafe {
    this.raw(buildString(block))
}

@HtmlTagMarker
fun HTMLTag.raw(content: String): Unit = consumer.onTagContentUnsafe {
    this.raw(content)
}

@HtmlTagMarker
fun FlowContent.divId(divId: String, classes: String? = null, block: DIV.() -> Unit = {}): Unit =
    DIV(attributesMapOf("class", classes), consumer).visit {
        id = divId
        block(this)
    }

@HtmlTagMarker
fun FlowContent.container(block: DIV.() -> Unit = {}) = div("container", block)

@HtmlTagMarker
fun FlowContent.fluidContainer(block: DIV.() -> Unit = {}) = div("container-fluid", block)

@HtmlTagMarker
fun HtmlBlockTag.insertImageGallery(
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    galleryName: String,
    configuration: WebsiteConfig,
    imageInformation: List<ImageInformation>
) = with(imageInformation) {
    if (isEmpty())
        return@with
    div {
        val isSingleImageGallery = imageInformation.size == 1
        classes = classes + "gallery center mt-3" + if (isSingleImageGallery) "single" else "row multiple"
        attributes["data-name"] = galleryName
        val additionalClasses = if (isSingleImageGallery)
            ""
        else
            "col-12 col-sm-6 col-lg-4 col-xl-3 col-xxl-2"
        imageInformation.forEach { curImageInfo ->
            insertLazyPicture(imageSizes, fallback, curImageInfo, configuration, additionalClasses)
        }
    }
}

fun HtmlBlockTag.insertImageGallery(
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    galleryName: String,
    configuration: WebsiteConfig,
    vararg imageInformation: ImageInformation
) = insertImageGallery(imageSizes, fallback, galleryName, configuration, listOf(*imageInformation))

fun HtmlBlockTag.insertLazyPicture(
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    curImageInfo: ImageInformation,
    configuration: WebsiteConfig,
    additionalClasses: String? = null
) {
    div("pic-holder " + (additionalClasses ?: "")) {
        div(PageGenerator.LAZYLOADER_CLASSNAME) {
            attributes["data-alt"] = curImageInfo.title
            attributes["data-id"] = curImageInfo.filename
            attributes["data-url"] = curImageInfo.getUrl(configuration)
            curImageInfo.thumbnailSizes.values.first().let {
                style = "padding-top: ${(it.height * 100f) / it.width}%"
            }
            imageSizes.forEachIndexed { idx, curSize ->
                val thumbnailSize = curImageInfo.thumbnailSizes[curSize] ?: error("Size $curSize not found!")
                attributes["data-$idx"] = """{
                    |"jpg":"${curImageInfo.getJpgUrl(configuration, curSize)}",
                    |"webp":"${curImageInfo.getWebPUrl(configuration, curSize)}",
                    |"w":${thumbnailSize.width},
                    |"h":${thumbnailSize.height}
                    |}""".trimMargin()
            }
            attributes["data-sizes"] = imageSizes.size.toString()
            noScript {
                img(curImageInfo.title, curImageInfo.getJpgUrl(configuration, fallback))
            }
        }
    }
}

private fun ImageInformation.getJpgUrl(configuration: WebsiteConfig, imageSize: ImageSize): String {
    return getUrl(configuration).substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".jpg"
}

private fun ImageInformation.getWebPUrl(configuration: WebsiteConfig, imageSize: ImageSize): String {
    return getUrl(configuration).substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".webp"
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.smallButtonLink(
    text: String,
    href: String,
    target: String = "_blank"
) =
    a(href, target, classes = "btn btn-primary btn-sm active") {
        attributes["role"] = "button"
        text(text)
    }

var DelegatingMap.value
    get() = this["value"]
    set(value) {
        if (value == null)
            this.remove("value")
        else
            this["value"] = value
    }

var DelegatingMap.itemprop
    get() = this["itemprop"]
    set(value) {
        if (value == null)
            this.remove("itemprop")
        else
            this["itemprop"] = value
    }

var DelegatingMap.itemscope
    get() = this["itemscope"]
    set(value) {
        if (value == null)
            this.remove("itemscope")
        else
            this["itemscope"] = value
    }

var DelegatingMap.itemtype
    get() = this["itemtype"]
    set(value) {
        if (value == null)
            this.remove("itemtype")
        else
            this["itemtype"] = value
    }

object Itemtypes {
    const val ARTICLE = "http://schema.org/Article"
}

var DelegatingMap.content
    get() = this["content"]
    set(value) {
        if (value == null)
            this.remove("content")
        else
            this["content"] = value
    }

fun HtmlBlockTag.renderCarousel(
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    id: String,
    changeMs: Int,
    filename: Array<out FilenameWithoutExtension>,
    imageFetcher: ImageFetcher,
    websiteConfig: WebsiteConfig
) {
    div("carousel slide") {
        this.id = id
        attributes["data-bs-interval"] = changeMs.toString()
        attributes["data-bs-ride"] = "carousel"
        div("carousel-inner") {
            filename.forEachIndexed { idx, filename ->
                div {
                    classes = classes + "carousel-item"
                    if (idx == 0)
                        classes = classes + "active"


                    insertLazyPicture(
                        imageSizes,
                        fallback,
                        imageFetcher(filename),
                        websiteConfig,
                        "d-block w-100"
                    )
                }
            }
        }

        a("#$id", classes = "carousel-control-prev") {
            role = "button"
            attributes["data-bs-slide"] = "prev"
            span("carousel-control-prev-icon") {
                attributes["aria-hidden"] = "true"
            }
            span("visually-hidden") { text("Vorheriges Bild") }
        }

        a("#$id", classes = "carousel-control-next") {
            role = "button"
            attributes["data-bs-slide"] = "next"
            span("carousel-control-next-icon") {
                attributes["aria-hidden"] = "true"
            }
            span("visually-hidden") { text("Nächstes Bild") }
        }
    }
}

@HtmlTagMarker
fun HtmlBlockTag.insertYoutube(codeOrLinkFragment: String, w: Int, h: Int, vararg additionalClasses: String) {
    p("ratio") {
        classes = classes + ("ratio-${w}x$h")
        if (additionalClasses.isNotEmpty())
            classes = classes + additionalClasses

        iframe(classes = "lazy") {
            attributes["data-src"] =
                if (codeOrLinkFragment.startsWith("http")) codeOrLinkFragment else "https://www.youtube-nocookie.com/embed/$codeOrLinkFragment"
            attributes["allow"] =
                "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            attributes["allowfullscreen"] = ""
        }
    }
}

@HtmlTagMarker
fun DIV.insertWartelisteInfo() {
    p("d-inline-block") { text("Wenn du dich für ein Shooting anmelden möchtest trage dich einfach direkt in die Warteliste ein:") }
    a("https://service.reisishot.pictures/waitlist", "_blank", "pl-2 btn btn-primary") {
        text("Zur Anmeldung gehen ")
        insertIcon(ReisishotIcons.LINK, "xs", "text-white")
    }
}

@HtmlTagMarker
fun FlowOrPhrasingContent.metadata(metadata: PageMetadata) {
    span("badge bg-light text-secondary text-pre-wrap") {
        metadata.edited?.let {
            time {
                attributes.itemprop = "dateModified"
                dateTime = pictures.reisishot.mise.backend.df_yyyy_MM_dd.format(it)

                span("font-weight-normal") {
                    text("Zuletzt aktualisiert am: ")
                }
                text(it)
            }
            text("  –  ")
        }
        time {
            attributes.itemprop = "datePublished"
            dateTime = pictures.reisishot.mise.backend.df_yyyy_MM_dd.format(metadata.created)
            span("font-weight-normal") {
                text("Veröffentlicht am: ")
            }
            text(metadata.created)
        }
    }
}

@HtmlTagMarker
fun Tag.text(date: Date) {
    text(df_dd_MM_YYYY.format(date))
}

fun <T : Appendable> T.appendUnformattedHtml() = appendHTML(false, true)
