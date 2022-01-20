@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package pictures.reisishot.mise.backend.generator.gallery.context

import at.reisishot.mise.backend.config.BuildingCache
import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.exifdata.ExifdataKey.CREATION_DATETIME
import kotlinx.html.DIV
import kotlinx.html.div
import kotlinx.html.p
import kotlinx.html.span
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.appendUnformattedHtml
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator.ImageSize.Companion as DefaultImageSize

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


    fun insertCarousel(id: String, changeMs: Int, vararg imageFilenames: FilenameWithoutExtension) = buildString {
        appendUnformattedHtml().div {
            renderCarousel(galleryGenerator, id, changeMs, imageFilenames, websiteConfig)
        }
    }

    @SuppressWarnings("unused")
    fun insertCarousel(changeMs: Int, vararg filename: FilenameWithoutExtension) =
        insertCarousel("carousel", changeMs, *filename)

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
                attributes["data-sizes"] = DefaultImageSize.values.toString()
            }
        }


        appendUnformattedHtml().div("bal-container") {
            val ratio = (h / w.toFloat())
            attributes["style"] = "width: 550px;height:${(550 * ratio).roundToInt()}px"
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
}
