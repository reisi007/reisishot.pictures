package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.exifdata.ExifdataKey
import kotlinx.html.*
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.*
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.generator.pages.PageMetadata
import pictures.reisishot.mise.backend.generator.pages.minimalistic.TargetPath
import pictures.reisishot.mise.backend.generator.testimonials.Testimonial
import pictures.reisishot.mise.backend.generator.testimonials.TestimonialLoader
import pictures.reisishot.mise.backend.html.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class TemplateApi(
    private val pageMetadata: PageMetadata?,
    private val targetPath: TargetPath,
    private val galleryGenerator: AbstractGalleryGenerator,
    private val cache: BuildingCache,
    private val websiteConfiguration: WebsiteConfiguration,
    private val testimonialLoader: TestimonialLoader
) {

    private val testimonials: Map<String, Testimonial>
        get() = testimonialLoader.load()


    @SuppressWarnings("unused")
    @JvmOverloads
    fun insertPicture(filenameWithoutExtension: FilenameWithoutExtension, classNames: String? = null) = buildString {
        appendUnformattedHtml().div {
            with(galleryGenerator.cache) {
                insertLazyPicture(
                    imageInformationData.getOrThrow(filenameWithoutExtension, targetPath),
                    websiteConfiguration,
                    "solo $classNames"
                )
            }
        }
    }


    @SuppressWarnings("unused")
    fun insertGallery(
        galleryName: String,
        vararg filenameWithoutExtension: FilenameWithoutExtension
    ): String {
        return with(galleryGenerator.cache) {
            filenameWithoutExtension.asSequence()
                .map {
                    imageInformationData.getOrThrow(it, targetPath)
                }.toList().let { imageInformations ->
                    buildString {
                        appendUnformattedHtml().div {
                            insertImageGallery(galleryName, websiteConfiguration, imageInformations)
                        }
                    }
                }
        }
    }

    @SuppressWarnings("unused")
    fun insertLink(type: String, key: String): String = cache.getLinkcacheEntryFor(websiteConfiguration, type, key)

    @SuppressWarnings("unused")
    fun insertLink(linktext: String, type: String, key: String): String = buildString {
        appendUnformattedHtml().a(insertLink(type, key)) {
            text(linktext)
        }
    }

    @SuppressWarnings("unused")
    fun insertSubalbumThumbnails(albumName: String?): String = buildString {
        appendUnformattedHtml().div {
            insertSubcategoryThumbnails(CategoryName(albumName ?: ""), websiteConfiguration, galleryGenerator)
        }
    }

    @SuppressWarnings("unused")
    fun insertCategoryOverview(vararg albumName: String) = buildString {
        if (albumName.isEmpty()) return@buildString
        appendUnformattedHtml().div {
            val albums = albumName.asSequence()
                .map { CategoryName(it) }
                .toCollection(LinkedHashSet())
            insertCategoryThumbnails(albums, websiteConfiguration, galleryGenerator)
        }
    }

    @SuppressWarnings("unused")
    @JvmOverloads
    fun insertTestimonial(name: String, mode: TestimonialMode = TestimonialMode.DEFAULT) = buildString {
        val testimonialsToDisplay = testimonials.getValue(name)
        appendUnformattedHtml().div {
            appendTestimonials(
                websiteConfiguration,
                targetPath,
                galleryGenerator,
                mode,
                false,
                testimonialsToDisplay,
            )
        }
    }


    @SuppressWarnings("unused")
    fun insertTestimonials(vararg testimonialTypes: String) = buildString {
        val testimonialsToDisplay = computeMatchingTestimonials(testimonialTypes)
        if (testimonialsToDisplay.isEmpty())
            return@buildString
        appendUnformattedHtml().div {
            appendTestimonials(
                websiteConfiguration,
                targetPath,
                galleryGenerator,
                TestimonialMode.DEFAULT,
                true,
                *testimonialsToDisplay
            )
        }

    }

    @SuppressWarnings("unused")
    fun insertTestimonialStatistics(vararg testimonialTypes: String) =
        buildString {
            val testimonialsToDisplay = computeMatchingTestimonials(testimonialTypes)
            if (testimonialsToDisplay.isEmpty())
                return@buildString
            appendUnformattedHtml().div {
                if (testimonialsToDisplay.isNotEmpty()) {
                    renderTestimonialStatistics(websiteConfiguration, testimonialsToDisplay)
                }
            }
        }

    @SuppressWarnings("unused")
    @JvmOverloads
    fun toKontaktformular(text: String = "Zum Kontaktformular") = buildString {
        appendUnformattedHtml().div("center") {
            a("#footer", classes = "btn btn-primary") {
                text(text)
            }
        }
    }

    private fun computeMatchingTestimonials(testimonialTypes: Array<out String>): Array<Testimonial> {
        var tmpTestimonials = testimonials.values.asSequence()
        if (testimonialTypes.isNotEmpty())
            tmpTestimonials = tmpTestimonials.filter { testimonialTypes.contains(it.type) }
        return tmpTestimonials
            .sortedByDescending { it.date }
            .toList()
            .toTypedArray()
    }

    @SuppressWarnings("unused")
    fun addMeta(): String {
        val metadata = pageMetadata ?: throw IllegalStateException("No page metadata specified for \"$targetPath\"!")
        return buildString {
            appendUnformattedHtml().div {
                metadata(metadata)
            }
        }
    }

    @SuppressWarnings("unused")
    fun insertCarousel(changeMs: Int, vararg filename: FilenameWithoutExtension) =
        insertCarousel("carousel", changeMs, *filename)

    @SuppressWarnings("unused")
    fun insertCarousel(id: String, changeMs: Int, vararg imageFilenames: FilenameWithoutExtension) = buildString {
        appendUnformattedHtml().div {
            renderCarousel(id, changeMs, imageFilenames, galleryGenerator.cache, targetPath, websiteConfiguration)
        }
    }

    @SuppressWarnings("unused")
    fun insertTextCarousel(changeMs: Int, vararg text: String) =
        insertTextCarousel("testimonials", changeMs, *text)

    @SuppressWarnings("unused")
    fun insertYoutube(codeOrLinkFragment: String, w: Int, h: Int) = buildString {
        appendUnformattedHtml().div {
            insertYoutube(codeOrLinkFragment, w, h)
        }
    }

    @SuppressWarnings("unused")
    fun insertTextCarousel(id: String, changeMs: Int, vararg text: String) = buildString {
        appendUnformattedHtml().div("carousel slide") {
            this.id = id
            attributes["data-bs-interval"] = changeMs.toString()
            attributes["data-bs-ride"] = "carousel"
            div("carousel-inner") {
                text.forEachIndexed { idx, cur ->
                    div {
                        classes = classes + "carousel-item"
                        if (idx == 0)
                            classes = classes + "active"

                        p {
                            span {
                                text("„$cur“")
                            }
                        }
                    }
                }
            }

            a("#$id", classes = "carousel-control-prev") {
                role = "button"
                attributes["data-bs-slide"] = "prev"
                span("carousel-control-prev-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("visually-hidden") { text("Vorheriger Text") }
            }

            a("#$id", classes = "carousel-control-next") {
                role = "button"
                attributes["data-bs-slide"] = "next"
                span("carousel-control-next-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("visually-hidden") { text("Nächster Text") }
            }
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
                attributes["data-sizes"] = AbstractThumbnailGenerator.ImageSize.values().size.toString()
            }
        }


        appendUnformattedHtml().div("bal-container") {
            val ratio = (h / w.toFloat())
            attributes["style"] = "width: 550px;height:${Math.round(550 * ratio)}px"
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

    private val galleryInfoDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")

    @SuppressWarnings("unused")
    fun insertGalleryInfo(): String = buildString {
        appendUnformattedHtml().p {
            val images = galleryGenerator.cache.imageInformationData.values
            val count = images.size
            val prep = images.asSequence()
                .map { it as? InternalImageInformation }
                .filterNotNull()
                .map { it.exifInformation.get(ExifdataKey.CREATION_DATETIME) }
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
    fun insertWartelisteInfo(): String = buildString {
        appendUnformattedHtml().div {
            insertWartelisteInfo()
        }
    }
}
