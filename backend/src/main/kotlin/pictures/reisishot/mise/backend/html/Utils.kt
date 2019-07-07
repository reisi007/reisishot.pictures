package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.ThumbnailGenerator


@HtmlTagMarker
fun HTMLTag.raw(block: StringBuilder.() -> Unit): Unit = consumer.onTagContentUnsafe {
    this.raw(buildString(block))
}

@HtmlTagMarker
fun HTMLTag.raw(content: String): Unit = consumer.onTagContentUnsafe {
    this.raw(content)
}

@HtmlTagMarker
fun FlowContent.div(divId: String, classes: String? = null, block: DIV.() -> Unit = {}): Unit =
    DIV(attributesMapOf("class", classes), consumer).visit {
        id = divId
        block(this)
    }

@HtmlTagMarker
fun FlowContent.container(block: DIV.() -> Unit = {}) = div("container", block)

@HtmlTagMarker
fun FlowContent.fluidContainer(block: DIV.() -> Unit = {}) = div("container-fluid", block)

@HtmlTagMarker
fun FlowContent.photoSwipeHtml() = div(classes = "pswp") {
    attributes["tabindex"] = "-1"
    attributes["role"] = "dialog"
    attributes["aria-hidden"] = "true"
    div(classes = "pswp__bg")
    div(classes = "pswp__scroll-wrap") {
        div(classes = "pswp__container") {
            repeat(3) {
                div(classes = "pswp__item")
            }
        }
        div(classes = "pswp__ui pswp__ui--hidden") {
            div(classes = "pswp__top-bar") {
                div(classes = "pswp__counter")

                button(classes = "pswp__button pswp__button--close") {
                    attributes["shortTitle"] = "Schließen (Esc)"
                }
                button(classes = "pswp__button pswp__button--fs") {
                    attributes["shortTitle"] = "Fullscreen anzeigen"
                }
                button(classes = "pswp__button pswp__button--zoom") {
                    attributes["shortTitle"] = "Zoomen"
                }
                button(classes = "pswp__button pswp__button--details") {
                    attributes["shortTitle"] = "Details"
                }
                div(classes = "pswp__preloader") {
                    div(classes = "pswp__preloader__icn") {
                        div(classes = "pswp__preloader__cut") {
                            div(classes = "pswp__preloader__donut")
                        }
                    }
                }
            }

            div(classes = "pswp__share-modal pswp__share-modal--hidden pswp__single-tap") {
                div(classes = "pswp__share-tooltip")
            }
            button(classes = "pswp__button pswp__button--arrow--left") {
                attributes["shortTitle"] = "Vorheriges Bild"
            }
            button(classes = "pswp__button pswp__button--arrow--right") {
                attributes["shortTitle"] = "Nächstes Bild"
            }
            div(classes = "pswp__caption") {
                div(classes = "pswp__caption__center")
            }
        }
    }
}

fun HtmlBlockTag.insertImageGallery(
    galleryName: String,
    vararg imageInformation: InternalImageInformation
) = with(imageInformation) {
    if (isEmpty())
        return@with
    div("gallery") {
        classes = classes + if (imageInformation.size == 1) "single" else "overview"
        attributes["data-name"] = galleryName
        imageInformation.forEach { curImageInfo ->
            picture(PageGenerator.LAZYLOADER_CLASSNAME) {
                val largeImageUrl = curImageInfo.thumbnailSizes.getHtmlUrl(ThumbnailGenerator.ImageSize.LARGE)
                attributes["style"] = "width: ${ThumbnailGenerator.ImageSize.LARGEST.longestSidePx}px"
                attributes["data-iesrc"] = largeImageUrl
                attributes["data-alt"] = curImageInfo.title
                attributes["data-url"] = curImageInfo.url

                ThumbnailGenerator.ImageSize.ORDERED.forEach { curSize ->
                    generateSourceTag(curImageInfo, curSize, largeImageUrl)
                }
            }
        }
    }
}

class PICTURE(classes: String? = null, consumer: TagConsumer<*>) :
    HTMLTag("picture", consumer, attributesMapOf("class", classes), inlineTag = false, emptyTag = false), HtmlBlockTag

@HtmlTagMarker
fun HtmlBlockTag.picture(classes: String? = null, block: PICTURE.() -> Unit = {}) =
    PICTURE(classes, consumer).visit(block)


@HtmlTagMarker
fun PICTURE.source(srcset: String, mediaQuery: String? = null, classes: String? = null, block: SOURCE.() -> Unit = {}) =
    SOURCE(
        attributesMapOf(
            "srcset", srcset,
            "media", mediaQuery,
            "classes", classes
        ), consumer
    ).visit(block)

private fun PICTURE.generateSourceTag(
    curImageInformation: InternalImageInformation,
    curSize: ThumbnailGenerator.ImageSize,
    largeImageUrl: String
) {
    val curSizeInfo = curImageInformation.thumbnailSizes[curSize] ?: return
    val smallerSizeInfo = curSize.smallerSize?.let { curImageInformation.thumbnailSizes[it] }
    curSizeInfo.let { (location1, width1, height1) ->

        source(
            srcset = getThumbnailUrlFromFilename(location1)
        ) {
            smallerSizeInfo?.let { (_, width2, height2) ->
                attributes["media"] = "(min-width: ${width2 + 1}px),(min-height: ${height2 + 1}px)"
            }
            attributes["data-w"] = width1.toString()
            attributes["data-h"] = height1.toString()
        }

        noScript {
            img(alt = curImageInformation.title, src = largeImageUrl)
        }
    }
}

private fun Map<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation>.getHtmlUrl(imageSize: ThumbnailGenerator.ImageSize): String =
    with(this.get(imageSize)?.filename) {
        if (this == null)
            throw IllegalStateException("Cannot get Url for this Thumbnail!")
        getThumbnailUrlFromFilename(this)
    }

private fun getThumbnailUrlFromFilename(filename: String): String =
    "/${ThumbnailGenerator.NAME_IMAGE_SUBFOLDER}/$filename"