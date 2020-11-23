package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import pictures.reisishot.mise.backend.generator.gallery.CategoryInformation
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize


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
        classes = classes + "gallery center mt-3" + if (isSingleImageGallery) "single" else "row multiple"
        attributes["data-name"] = galleryName
        val additionalClasses = if (isSingleImageGallery)
          ""
        else
            "col-12 col-sm-6 col-lg-4 col-xl-3"
        imageInformation.forEach { curImageInfo ->
            insertLazyPicture(curImageInfo, additionalClasses)
        }
    }
}

fun HtmlBlockTag.insertImageGallery(
        galleryName: String,
        vararg imageInformation: ImageInformation
) = insertImageGallery(galleryName, listOf(*imageInformation))

internal fun HtmlBlockTag.insertLazyPicture(
        curImageInfo: ImageInformation,
        additionalClasses: String? = null
) {
    div("pic-holder " + (additionalClasses ?: "")) {
        div(PageGenerator.LAZYLOADER_CLASSNAME) {
            attributes["data-alt"] = curImageInfo.title
            attributes["data-id"] = curImageInfo.filename
            attributes["data-url"] = curImageInfo.href
            curImageInfo.thumbnailSizes.values.first().let {
                style = "padding-top: ${(it.height * 100f) / it.width}%"
            }
            ImageSize.values().forEachIndexed { idx, curSize ->
                val thumbnailSize = curImageInfo.thumbnailSizes.getValue(curSize)
                attributes["data-$idx"] = """{"jpg":"${curImageInfo.getJpgUrl(curSize)}","webp":"${curImageInfo.getWebPUrl(curSize)}","w":${thumbnailSize.width},"h":${thumbnailSize.height}}"""
            }
            attributes["data-sizes"] = ImageSize.values().size.toString()
            noScript {
                img(curImageInfo.title, curImageInfo.getJpgUrl(ImageSize.LARGEST))
            }
        }
    }
}

private fun ImageInformation.getJpgUrl(imageSize: ImageSize): String {
    return href.substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".jpg"
}

private fun ImageInformation.getWebPUrl(imageSize: ImageSize): String {
    return href.substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".webp"
}

internal fun HtmlBlockTag.insertSubcategoryThumbnail(
        categoryInformation: CategoryInformation,
        imageInformation: ImageInformation
) {
    a("/gallery/categories/${categoryInformation.urlFragment}", classes = "card") {
        div("card-img-top") {
            insertLazyPicture(imageInformation)
        }
        div("card-body") {
            h4("card-title") {
                text(categoryInformation.displayName)
            }
        }
    }
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.smallButtonLink(text: String, href: String, target: String = "_blank") = a(href, target, classes = "btn btn-primary btn-sm active") {
    attributes["role"] = "button"
    text(text)
}