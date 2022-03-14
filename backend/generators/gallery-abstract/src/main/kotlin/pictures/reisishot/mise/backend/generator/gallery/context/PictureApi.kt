@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package pictures.reisishot.mise.backend.generator.gallery.context

import kotlinx.html.div
import kotlinx.html.p
import kotlinx.html.span
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.html.appendUnformattedHtml
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.exifdata.ExifdataKey.CREATION_DATETIME
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@WebsiteConfigBuilderDsl
fun AbstractGalleryGenerator.createPictureApi(): Pair<String, VelocityTemplateObjectCreator> =
    "pictures" to { pageMetadata, websiteConfig, cache ->
        PictureApi(pageMetadata, cache, websiteConfig, this)
    }

internal class PictureApi(
    private val pageMetadata: PageMetadata?,
    private val cache: BuildingCache,
    private val websiteConfig: WebsiteConfig,
    private val galleryGenerator: AbstractGalleryGenerator
) : TemplateObject {
    private val galleryInfoDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")

    @JvmOverloads
    fun insertPicture(filenameWithoutExtension: FilenameWithoutExtension, classNames: String? = null) = buildString {
        appendUnformattedHtml().div {
            insertLazyPicture(
                galleryGenerator.cache.imageInformationData.getValue(filenameWithoutExtension),
                websiteConfig,
                "solo $classNames"
            )
        }
    }

    fun insertGallery(
        galleryName: String,
        vararg filenameWithoutExtension: FilenameWithoutExtension
    ): String {
        return filenameWithoutExtension.asSequence()
            .map { galleryGenerator.cache.imageInformationData.getValue(it) }
            .toList()
            .let {
                buildString {
                    appendUnformattedHtml().div {
                        insertImageGallery(galleryName, websiteConfig, it)
                    }
                }
            }
    }

    fun insertImageCarousel(id: String, changeMs: Int, vararg imageFilenames: FilenameWithoutExtension) = buildString {
        appendUnformattedHtml().div {
            renderImageCarousel(galleryGenerator, id, changeMs, imageFilenames, websiteConfig)
        }
    }

    @SuppressWarnings("unused")
    fun insertImageCarousel(changeMs: Int, vararg filename: FilenameWithoutExtension) =
        insertImageCarousel("carousel", changeMs, *filename)

    fun insertGalleryInfo(): String = buildString {
        appendUnformattedHtml().p {
            val images = galleryGenerator.cache.imageInformationData.values
            val count = images.size
            val prep = images.asSequence()
                .map { it as? InternalImageInformation }
                .filterNotNull()
                .map { it.exifInformation[CREATION_DATETIME] }
                .filterNotNull()
                .map { ZonedDateTime.parse(it) }

            val lastImage = prep.maxOrNull()
            val firstImage = prep.minOrNull()

            text("Auf dieser Webseite sind insgesamt ")
            text(count)
            text(" Bilder")
            if (firstImage != null && lastImage != null) {
                text(" aufgenommen zwischen ")
                text(galleryInfoDateTimeFormatter.format(firstImage))
                text(" und ")
                text(galleryInfoDateTimeFormatter.format(lastImage))
            }
            text(" zu sehen.")
        }
    }

    @SuppressWarnings("unused")
    fun insertSlidingImages(filename: String): String {
        return insertSlidingImages(filename + "o", filename)
    }

    private fun insertSlidingImages(originalImage: String, editedImage: String): String = buildString {
        val cache = galleryGenerator.cache
        val editedImageInfo = cache.imageInformationData.getValue(editedImage)
        val largestSize = editedImageInfo.thumbnailSizes.getValue(AbstractThumbnailGenerator.ImageSize.LARGEST)
        appendUnformattedHtml().div("bal-container") {
            val ratio = (largestSize.height / largestSize.width.toFloat())
            attributes["data-ratio"] = ratio.toString()

            div("bal-after") {
                insertLazyPicture(editedImageInfo, websiteConfig)
                div("bal-afterPosition afterLabel") {
                    text("Bearbeitet")
                }
            }
            div("bal-before") {
                div("bal-before-inset") {
                    insertLazyPicture(cache.imageInformationData.getValue(originalImage), websiteConfig)
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
}
