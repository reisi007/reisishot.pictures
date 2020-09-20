package pictures.reisishot.mise.backend.generator.pages

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
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.insertLazyPicture
import pictures.reisishot.mise.backend.html.raw

class TemplateApi(
        private val targetPath: TargetPath,
        private val galleryGenerator: AbstractGalleryGenerator,
        private val cache: BuildingCache,
        private val websiteConfiguration: WebsiteConfiguration
) {
    private var privateHasGallery = false
    val hasGallery
        get() = privateHasGallery
    private val testimonials by lazy {
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
            val cssClasses = classNames?.split(" ") ?: listOf()
            with(galleryGenerator.cache) {
                insertLazyPicture(imageInformationData.getOrThrow(filenameWithoutExtension), cssClasses)
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
                testimonialsToDisplay.forEach { testimonial ->
                    div("col-12 col-lg-5 card border-dark") {
                        with(galleryGenerator.cache) {
                            insertLazyPicture(imageInformationData.getOrThrow(testimonial.image), listOf("card-img-top"))
                        }
                        div("card-body text-dark") {
                            h5("card-title") {
                                text(testimonial.name)
                                br()
                                small("text-muted") { text(testimonial.dateFormatted()) }
                            }
                            p("card-text") {
                                raw(testimonial.text.replace("\n", "<br/>"))
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
    fun insertSlidingImages(filename: String, w: Int, h: Int) = buildString {
        appendHTML(false, true).div("bal-container") {
            val ratio = (h / w.toFloat())
            attributes["style"] = "width: 550px;height:${Math.round(550 * ratio)}px"
            attributes["data-ratio"] = ratio.toString()
            div("bal-after") {
                img("Bearbeitet", "https://images.reisishot.pictures/?url=${filename}b.jpg&w=550&q=70")
                div("bal-afterPosition afterLabel") {
                    text("Bearbeitet")
                }
            }
            div("bal-before") {
                div("bal-before-inset") {
                    img("Bearbeitet", "https://images.reisishot.pictures/?url=${filename}o.jpg&w=400&q=70")
                    div("bal-beforePosition beforeLabel") {
                        text("Aus der Kamera")
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