package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import pictures.reisishot.mise.backend.generator.gallery.CategoryInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.simpleName
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.Companion.NAME_IMAGE_SUBFOLDER
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize.Companion.LARGEST
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize.Companion.ORDERED
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize.LARGE
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ThumbnailInformation


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
fun FlowContent.photoSwipeHtml() = div("pswp") {
    attributes["tabindex"] = "-1"
    attributes["role"] = "dialog"
    attributes["aria-hidden"] = "true"
    div("pswp__bg")
    div("pswp__scroll-wrap") {
        div("pswp__container") {
            repeat(3) {
                div("pswp__item")
            }
        }
        div("pswp__ui pswp__ui--hidden") {
            div("pswp__top-bar") {
                div("pswp__counter")

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
                div("pswp__preloader") {
                    div("pswp__preloader__icn") {
                        div("pswp__preloader__cut") {
                            div("pswp__preloader__donut")
                        }
                    }
                }
            }

            div("pswp__share-modal pswp__share-modal--hidden pswp__single-tap") {
                div("pswp__share-tooltip")
            }
            button(classes = "pswp__button pswp__button--arrow--left") {
                attributes["shortTitle"] = "Vorheriges Bild"
            }
            button(classes = "pswp__button pswp__button--arrow--right") {
                attributes["shortTitle"] = "Nächstes Bild"
            }
            div("pswp__caption") {
                div("pswp__caption__center")
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
    divId("gallery") {
        classes = classes + if (imageInformation.size == 1) "single" else "overview"
        attributes["data-name"] = galleryName
        imageInformation.forEach { curImageInfo ->
            insertLazyPicture(curImageInfo)
        }
    }
}

internal fun FlowOrInteractiveOrPhrasingContent.insertLazyPicture(
        curImageInfo: InternalImageInformation,
        additionalClasses: List<String> = emptyList()
) {
    picture(PageGenerator.LAZYLOADER_CLASSNAME) {
        if (additionalClasses.isNotEmpty())
            classes = classes + additionalClasses
        val largeImageUrl = curImageInfo.thumbnailSizes.getHtmlUrl(LARGE)
        attributes["style"] = "width: ${LARGEST.longestSidePx}px"
        attributes["data-iesrc"] = largeImageUrl
        attributes["data-alt"] = curImageInfo.title
        attributes["data-url"] = curImageInfo.url

        ORDERED.forEach { curSize ->
            generateSourceTag(curImageInfo, curSize, largeImageUrl)
        }
    }
}

internal fun DIV.insertSubcategoryThumbnail(
        categoryInformation: CategoryInformation,
        imageInformation: InternalImageInformation
) {
    div("card") {
        a(href = "/gallery/categories/${categoryInformation.urlFragment}") {
            insertLazyPicture(imageInformation, listOf("card-img-top"))
            div("card-body") {
                h4("card-title") {
                    text(categoryInformation.complexName.simpleName)
                }
            }
        }
    }
}

class PICTURE(classes: String? = null, consumer: TagConsumer<*>) :
        HTMLTag("picture", consumer, attributesMapOf("class", classes), inlineTag = false, emptyTag = false), HtmlBlockTag

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.picture(classes: String? = null, block: PICTURE.() -> Unit = {}) =
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
        curSize: ImageSize,
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

private fun Map<ImageSize, ThumbnailInformation>.getHtmlUrl(imageSize: ImageSize): String =
        with(this.get(imageSize)?.filename) {
            if (this == null)
                throw IllegalStateException("Cannot get Url for this Thumbnail!")
            getThumbnailUrlFromFilename(this)
        }

private fun getThumbnailUrlFromFilename(filename: String): String =
        "/${NAME_IMAGE_SUBFOLDER}/$filename"