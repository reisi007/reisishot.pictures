package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.commons.filenameWithoutExtension
import at.reisishot.mise.commons.withChild
import at.reisishot.mise.config.parseConfig
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.insertCategoryThumbnails
import pictures.reisishot.mise.backend.generator.gallery.insertSubcategoryThumbnails
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.generator.pages.Testimonal
import pictures.reisishot.mise.backend.generator.pages.dateFormatted
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.insertLazyPicture
import pictures.reisishot.mise.backend.html.raw
import java.nio.file.Path
import kotlin.math.roundToInt

class TemplateApi(
        private val targetPath: TargetPath,
        private val galleryGenerator: AbstractGalleryGenerator,
        private val cache: BuildingCache,
        private val websiteConfiguration: WebsiteConfiguration
) {
    private var privateHasGallery = false
    val hasGallery
        get() = privateHasGallery

    private val testimonials: List<Testimonal> by lazy {
        (websiteConfiguration.inPath withChild "testimonials.conf").parseConfig<List<Testimonal>>("testimonials")
                ?: throw IllegalStateException("Could not load testimonials")
    }


    private fun Map<FilenameWithoutExtension, ImageInformation>.getOrThrow(key: FilenameWithoutExtension) =
            this[key]
                    ?: throw IllegalStateException("Cannot find picture with filename \"$key\" (used in ${targetPath.filenameWithoutExtension})!")

    @SuppressWarnings("unused")
    @JvmOverloads
    fun insertPicture(filenameWithoutExtension: FilenameWithoutExtension, classNames: String? = null) = buildString {
        appendHTML(prettyPrint = false, xhtmlCompatible = true).div {
            with(galleryGenerator.cache) {
                insertLazyPicture(imageInformationData.getOrThrow(filenameWithoutExtension), classNames)
            }
        }
    }

    @SuppressWarnings("unused")
    fun insertGallery(
            galleryName: String,
            vararg filenameWithoutExtension: FilenameWithoutExtension
    ): String {
        privateHasGallery = privateHasGallery || filenameWithoutExtension.isNotEmpty()
        return with(galleryGenerator.cache) {
            filenameWithoutExtension.asSequence()
                    .map {
                        imageInformationData.getOrThrow(it)
                    }.toList().let { imageInformations ->
                        buildString {
                            appendHTML(prettyPrint = false, xhtmlCompatible = true).div {
                                insertImageGallery(galleryName, imageInformations)
                            }
                        }
                    }
        }
    }

    @SuppressWarnings("unused")
    fun insertLink(type: String, key: String): String = cache.getLinkcacheEntryFor(websiteConfiguration, type, key)

    @SuppressWarnings("unused")
    fun insertLink(linktext: String, type: String, key: String): String = buildString {
        appendHTML(false, true).a(insertLink(type, key)) {
            text(linktext)
        }
    }

    @SuppressWarnings("unused")
    fun insertSubalbumThumbnails(albumName: String?): String = buildString {
        appendHTML(false, true).div {
            insertSubcategoryThumbnails(CategoryName(albumName ?: ""), galleryGenerator)
        }
    }

    @SuppressWarnings("unused")
    fun insertCategoryOverview(vararg albumName: String) = buildString {
        if (albumName.isEmpty()) return@buildString
        appendHTML(false, true).div {
            val albums = albumName.asSequence()
                    .map { CategoryName(it) }
                    .toCollection(LinkedHashSet())
            insertCategoryThumbnails(albums, galleryGenerator);
        }
    }

    @SuppressWarnings("unused")
    fun insertTestimonials(vararg testimonialTypes: String) = buildString {
        val testimonialsToDisplay = computeMatchingTestimonials(testimonialTypes)
        if (testimonialsToDisplay.isEmpty())
            return@buildString
        appendHTML(false, true).div {
            div("container-flex reviews") {
                attributes["data-partial"] = "testimonials"
                attributes["data-initial"] = "4"
                attributes["data-step"] = "2"
                testimonialsToDisplay.forEach { testimonial ->
                    div("col-12 col-lg-5 card border-dark") {
                        with(galleryGenerator.cache) {
                            insertLazyPicture(imageInformationData.getOrThrow(testimonial.image), "card-img-top")
                        }
                        div("card-body text-dark") {
                            h5("card-title") {
                                text(testimonial.name)
                                br()
                                small("text-muted") { text(testimonial.dateFormatted()) }
                            }
                            div("card-text") {
                                raw("<p>" + testimonial.text.replace("\n", "</p><p>") + "</p>")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun computeMatchingTestimonials(testimonialTypes: Array<out String>): List<Testimonal> {
        var tmpTestimonials = testimonials.asSequence()
        if (testimonialTypes.isNotEmpty())
            tmpTestimonials = tmpTestimonials.filter { testimonialTypes.contains(it.type) }
        return tmpTestimonials
                .sortedByDescending { it.date }
                .toList()
    }

    @SuppressWarnings("unused")
    fun insertCarousel(changeMs: Int, vararg filename: String) = buildString {
        appendHTML(false, true).div("carousel slide") {
            id = "carousel"
            attributes["data-interval"] = changeMs.toString()
            attributes["data-ride"] = "carousel"
            div("carousel-inner") {
                filename.forEachIndexed { idx, filename ->
                    div {
                        classes = classes + "carousel-item"
                        if (idx == 0)
                            classes = classes + "active"

                        with(galleryGenerator.cache) {
                            insertLazyPicture(imageInformationData.getOrThrow(filename), "d-block w-100")
                        }
                    }
                }
            }

            a("#carousel", classes = "carousel-control-prev") {
                role = "button"
                attributes["data-slide"] = "prev"
                span("carousel-control-prev-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("sr-only") { text("Vorheriges Bild") }
            }

            a("#carousel", classes = "carousel-control-next") {
                role = "button"
                attributes["data-slide"] = "next"
                span("carousel-control-next-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("sr-only") { text("Nächstes Bild") }
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
                    attributes["data-$idx"] = """{"jpg":"${getJpgUrl(filename, size)}","webp":"${getWebPUrl(filename, size)}","w":$iw,"h":$ih}"""
                }
                attributes["data-sizes"] = AbstractThumbnailGenerator.ImageSize.values().size.toString()
            }
        }


        appendHTML(false, true).div("bal-container") {
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

}

typealias SourcePath = Path;
typealias TargetPath = Path;
typealias PageGeneratorInfo = Triple<SourcePath, TargetPath, String/*Title*/>
typealias Yaml = Map<String, List<String>>