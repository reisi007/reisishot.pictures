@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package pictures.reisishot.mise.backend.generator.gallery.context

import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.commons.FilenameWithoutExtension
import kotlinx.html.*
import pictures.reisishot.mise.backend.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator.ImageSize.Companion as DefaultImageSize


fun HtmlBlockTag.insertLazyPicture(
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
            DefaultImageSize.values.forEachIndexed { idx, curSize ->
                val thumbnailSize = curImageInfo.thumbnailSizes[curSize] ?: error("Size $curSize not found!")
                attributes["data-$idx"] = """{
                    |"jpg":"${curImageInfo.getJpgUrl(configuration, curSize)}",
                    |"webp":"${curImageInfo.getWebPUrl(configuration, curSize)}",
                    |"w":${thumbnailSize.width},
                    |"h":${thumbnailSize.height}
                    |}""".trimMargin()
            }
            attributes["data-sizes"] = DefaultImageSize.values.size.toString()
            noScript {
                img(curImageInfo.title, curImageInfo.getJpgUrl(configuration, DefaultImageSize.LARGEST))
            }
        }
    }
}

private fun ImageInformation.getJpgUrl(
    configuration: WebsiteConfig,
    imageSize: AbstractThumbnailGenerator.ImageSize
): String {
    return getUrl(configuration).substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".jpg"
}

private fun ImageInformation.getWebPUrl(
    configuration: WebsiteConfig,
    imageSize: AbstractThumbnailGenerator.ImageSize
): String {
    return getUrl(configuration).substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".webp"
}

fun HtmlBlockTag.renderCarousel(
    galleryGenerator: AbstractGalleryGenerator,
    id: String,
    changeMs: Int,
    filename: Array<out FilenameWithoutExtension>,
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
                        galleryGenerator.cache.imageInformationData.getValue(filename),
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
fun HtmlBlockTag.insertImageGallery(
    galleryName: String,
    configuration: WebsiteConfig,
    imageInformation: ImageInformation
): Unit =
    insertImageGallery(galleryName, configuration, listOf(imageInformation))

@HtmlTagMarker
fun HtmlBlockTag.insertImageGallery(
    galleryName: String,
    configuration: WebsiteConfig,
    imageInformation: List<ImageInformation>
): Unit = with(imageInformation) {
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
            insertLazyPicture(curImageInfo, configuration, additionalClasses)
        }
    }
}