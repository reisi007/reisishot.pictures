package pictures.reisishot.mise.backend.generator.gallery

import kotlinx.html.*
import pictures.reisishot.mise.backend.generator.gallery.ThumbnailGenerator.ImageSize.LARGE
import pictures.reisishot.mise.backend.html.PageGenerator


interface ImageInformation {
    val filenameWithoutExtension: FilenameWithoutExtension
    val url: String
    val title: String
    val tags: Set<TagName>
    val exifInformation: Map<ExifdataKey, String>
    val thumbnailSizes: Map<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation>
}

data class InternalImageInformation(
    override val filenameWithoutExtension: FilenameWithoutExtension,
    override val url: String,
    override val title: String,
    override val tags: Set<TagName>,
    override val exifInformation: Map<ExifdataKey, String>,
    override val thumbnailSizes: Map<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation>,
    val categories: MutableSet<CategoryInformation> = mutableSetOf()
) : ImageInformation

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
                val largeImageUrl = curImageInfo.thumbnailSizes.getHtmlUrl(LARGE)

                attributes["data-iesrc"] = largeImageUrl
                attributes["data-alt"] = curImageInfo.title

                ThumbnailGenerator.ImageSize.ORDERED.forEach { curSize ->
                    generateSourceTag(curImageInfo, curSize, largeImageUrl)
                }
            }
        }
    }
}

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