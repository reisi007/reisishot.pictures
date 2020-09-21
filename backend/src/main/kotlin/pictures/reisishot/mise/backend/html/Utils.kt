package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import pictures.reisishot.mise.backend.generator.gallery.CategoryInformation
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize.Companion.ORDERED
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize.Companion.getSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.scaleToHeight


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
        imageInformation: List<ImageInformation>) = with(imageInformation) {
    if (isEmpty())
        return@with
    div {
        val isSingleImageGallery = imageInformation.size == 1
        classes = classes + "gallery " + if (isSingleImageGallery) "single" else "row multiple"
        attributes["data-name"] = galleryName
        val additionalClasses = if (isSingleImageGallery) listOf()
        else listOf("col-12", "col-sm-6", "col-lg-4", "col-xl-3")
        imageInformation.forEach { curImageInfo ->
            insertLazyPicture(curImageInfo, additionalClasses)
        }
    }
}

fun HtmlBlockTag.insertImageGallery(
        galleryName: String,
        vararg imageInformation: ImageInformation
) = insertImageGallery(galleryName, listOf(*imageInformation))

internal fun FlowOrInteractiveOrPhrasingContent.insertLazyPicture(
        curImageInfo: ImageInformation,
        additionalClasses: List<String> = emptyList()
) {
    picture(PageGenerator.LAZYLOADER_CLASSNAME) {
        if (additionalClasses.isNotEmpty())
            classes = classes + additionalClasses
        val desiredHeight = 300
        val imageSize = getSize(desiredHeight)

        val largeImageUrl = curImageInfo.getHtmlUrl(imageSize)
        val thumbnailInformation = curImageInfo.thumbnailSizes.getValue(imageSize).scaleToHeight(desiredHeight)
        attributes["style"] = "width: ${thumbnailInformation.width}px; height: ${thumbnailInformation.height}px;"
        attributes["data-iesrc"] = largeImageUrl
        attributes["data-alt"] = curImageInfo.title
        attributes["data-id"] = curImageInfo.filename
        attributes["data-url"] = curImageInfo.href

        ORDERED.forEach { curSize ->
            generateSourceTag(curImageInfo, curSize)
        }

        noScript {
            img(alt = curImageInfo.title, src = largeImageUrl)
        }
    }
}

private fun ImageInformation.getHtmlUrl(imageSize: ImageSize): String {
    return href.substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".jpg"
}

internal fun HtmlBlockTag.insertSubcategoryThumbnail(
        categoryInformation: CategoryInformation,
        imageInformation: ImageInformation
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
        curImageInformation: ImageInformation,
        curSize: ImageSize
) {
    val curSizeInfo = curImageInformation.thumbnailSizes[curSize] ?: return
    val smallerSizeInfo = curSize.smallerSize?.let { curImageInformation.thumbnailSizes[it] }
    curSizeInfo.let { (_, width1, height1) ->

        source(
                srcset = curImageInformation.getHtmlUrl(curSize)
        ) {
            smallerSizeInfo?.let { (_, width2, height2) ->
                attributes["media"] = "(min-width: ${width2}px),(min-height: ${height2}px)"
            }
            attributes["data-w"] = width1.toString()
            attributes["data-h"] = height1.toString()
        }
    }
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.smallButtonLink(text: String, href: String, target: String = "_blank") = a(href, target, classes = "btn btn-primary btn-sm active") {
    attributes["role"] = "button"
    text(text)
}