package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import pictures.reisishot.mise.backend.generator.gallery.CategoryInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.Companion.NAME_IMAGE_SUBFOLDER
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize.Companion.ORDERED
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize.Companion.getSize
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

fun HtmlBlockTag.insertImageGallery(
        galleryName: String,
        vararg imageInformation: InternalImageInformation
) = with(imageInformation) {
    if (isEmpty())
        return@with
    div {
        classes = classes + "gallery" + if (imageInformation.size == 1) "single" else "overview"
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
        val imageSize = getSize(300)
        val largeImageUrl = curImageInfo.thumbnailSizes.getHtmlUrl(imageSize)
        val thumbnailInformation = curImageInfo.thumbnailSizes.getValue(imageSize)
        attributes["style"] = "width: ${thumbnailInformation.width}px; height: ${thumbnailInformation.height}px;"
        attributes["data-iesrc"] = largeImageUrl
        attributes["data-alt"] = curImageInfo.title
        attributes["data-url"] = curImageInfo.url

        ORDERED.forEach { curSize ->
            generateSourceTag(curImageInfo, curSize)
        }

        noScript {
            img(alt = curImageInfo.title, src = largeImageUrl)
        }
    }
}

internal fun HtmlBlockTag.insertSubcategoryThumbnail(
        categoryInformation: CategoryInformation,
        imageInformation: InternalImageInformation
) {
    div("card") {
        a(href = "/gallery/categories/${categoryInformation.urlFragment}") {
            insertLazyPicture(imageInformation, listOf("card-img-top"))
            div("card-body") {
                h4("card-title") {
                    text(categoryInformation.displayName)
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
        curSize: ImageSize
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
    }
}

private fun Map<ImageSize, ThumbnailInformation>.getHtmlUrl(imageSize: ImageSize): String =
        with(this[imageSize]?.filename) {
            if (this == null)
                throw IllegalStateException("Cannot get Url for this Thumbnail!")
            getThumbnailUrlFromFilename(this)
        }

private fun getThumbnailUrlFromFilename(filename: String): String =
        "/${NAME_IMAGE_SUBFOLDER}/$filename"

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.smallButtonLink(text: String, href: String, target: String = "_blank") = a(href, target, classes = "btn btn-primary btn-sm active") {
    attributes["role"] = "button"
    text(text)
}