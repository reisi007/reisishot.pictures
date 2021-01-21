package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.commons.*
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.*
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.generator.pages.Testimonal
import pictures.reisishot.mise.backend.html.*
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser.markdown2Html
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashSet
import kotlin.math.roundToInt
import kotlin.streams.asSequence

class TemplateApi(
        private val pageMetadata: PageMetadata?,
        private val targetPath: TargetPath,
        private val galleryGenerator: AbstractGalleryGenerator,
        private val cache: BuildingCache,
        private val websiteConfiguration: WebsiteConfiguration
) {

    private val testimonials: Map<String, Testimonal> by lazy {
        Files.list(websiteConfiguration.inPath withChild "reviews")
                .asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.hasExtension({ it.isMarkdownPart("review") }) }
                .map { path ->
                    val yamlContainer = AbstractYamlFrontMatterVisitor()
                    val html = Files.newBufferedReader(path, StandardCharsets.UTF_8).use {
                        it.markdown2Html(yamlContainer)
                    }
                    path.fileName.toString().substringBefore('.') to yamlContainer.data.createTestimonial(path, html)
                }
                .toMap()
    }

    private fun Yaml.createTestimonial(p: Path, contentHtml: String): Testimonal {
        val imageFilename = getString("image")
        val personName = getString("name")
        val date = getString("date")
        val type = getString("type")
        if (imageFilename == null || personName == null || date == null || type == null)
            throw IllegalStateException("Das Testimonial in $p ist nicht vollständig!")
        return Testimonal(imageFilename, personName, date, type, contentHtml)
    }


    @SuppressWarnings("unused")
    @JvmOverloads
    fun insertPicture(filenameWithoutExtension: FilenameWithoutExtension, classNames: String? = null) = buildString {
        appendHTML(prettyPrint = false, xhtmlCompatible = true).div {
            with(galleryGenerator.cache) {
                insertLazyPicture(imageInformationData.getOrThrow(filenameWithoutExtension, targetPath), websiteConfiguration, "solo $classNames")
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
                            appendHTML(prettyPrint = false, xhtmlCompatible = true).div {
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
        appendHTML(false, true).a(insertLink(type, key)) {
            text(linktext)
        }
    }

    @SuppressWarnings("unused")
    fun insertSubalbumThumbnails(albumName: String?): String = buildString {
        appendHTML(false, true).div {
            insertSubcategoryThumbnails(CategoryName(albumName ?: ""), websiteConfiguration, galleryGenerator)
        }
    }

    @SuppressWarnings("unused")
    fun insertCategoryOverview(vararg albumName: String) = buildString {
        if (albumName.isEmpty()) return@buildString
        appendHTML(false, true).div {
            val albums = albumName.asSequence()
                    .map { CategoryName(it) }
                    .toCollection(LinkedHashSet())
            insertCategoryThumbnails(albums, websiteConfiguration, galleryGenerator);
        }
    }

    @SuppressWarnings("unused")
    fun insertSingleTestimonial(name: String) = buildString {
        val testimonialsToDisplay = testimonials.getValue(name)
        appendTestimonials(websiteConfiguration, targetPath, galleryGenerator, testimonialsToDisplay)

    }

    @SuppressWarnings("unused")
    fun insertTestimonials(vararg testimonialTypes: String) = buildString {
        val testimonialsToDisplay = computeMatchingTestimonials(testimonialTypes)
        if (testimonialsToDisplay.isEmpty())
            return@buildString
        appendTestimonials(websiteConfiguration, targetPath, galleryGenerator, *testimonialsToDisplay)
    }

    private fun computeMatchingTestimonials(testimonialTypes: Array<out String>): Array<Testimonal> {
        var tmpTestimonials = testimonials.values.asSequence()
        if (testimonialTypes.isNotEmpty())
            tmpTestimonials = tmpTestimonials.filter { testimonialTypes.contains(it.type) }
        return tmpTestimonials
                .sortedByDescending { it.date }
                .toList()
                .toTypedArray()
    }

    @SuppressWarnings("unused")
    fun addMeta() {
        val metadata = pageMetadata ?: throw IllegalStateException("No page metadata specified for \"$targetPath\"!")
        buildString {
            appendHTML(false, true).span {
                metadata(metadata)
            }
        }
    }

    @SuppressWarnings("unused")
    fun insertCarousel(changeMs: Int, vararg filename: String) =
            insertCarousel("carousel", changeMs, *filename)


    fun insertCarousel(id: String, changeMs: Int, vararg filename: String) = buildString {
        appendHTML(false, true).div("carousel slide") {
            this.id = id
            attributes["data-interval"] = changeMs.toString()
            attributes["data-ride"] = "carousel"
            div("carousel-inner") {
                filename.forEachIndexed { idx, filename ->
                    div {
                        classes = classes + "carousel-item"
                        if (idx == 0)
                            classes = classes + "active"

                        with(galleryGenerator.cache) {
                            insertLazyPicture(imageInformationData.getOrThrow(filename, targetPath), websiteConfiguration, "d-block w-100")
                        }
                    }
                }
            }

            a("#$id", classes = "carousel-control-prev") {
                role = "button"
                attributes["data-slide"] = "prev"
                span("carousel-control-prev-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("sr-only") { text("Vorheriges Bild") }
            }

            a("#$id", classes = "carousel-control-next") {
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
    fun insertTextCarousel(changeMs: Int, vararg text: String) =
            insertTextCarousel("testimonials", changeMs, *text)


    fun insertTextCarousel(id: String, changeMs: Int, vararg text: String) = buildString {
        appendHTML(false, true).div("carousel slide") {
            this.id = id
            attributes["data-interval"] = changeMs.toString()
            attributes["data-ride"] = "carousel"
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
                attributes["data-slide"] = "prev"
                span("carousel-control-prev-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("sr-only") { text("Vorheriges Bild") }
            }

            a("#$id", classes = "carousel-control-next") {
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
typealias FormattedDate = String

private val yamlDateParser = SimpleDateFormat("yyyyMMdd")


fun Yaml.getString(key: String): String? {
    val value = getOrDefault(key, null)
    return value?.firstOrNull()?.trim()
}

fun Yaml.getOrder() = getString("order")

data class PageMetadata(
        val order: String,
        val created: Date,
        val edited: Date?
)

fun Yaml.asPageMetadata(): PageMetadata? {
    val order = getOrder()
    val created = getString("created")
    val edited = getString("edit")

    val computedOrder = order ?: created ?: edited
    // Ensure we have all required fields
    if (computedOrder == null || created == null)
        return null
    return PageMetadata(
            computedOrder,
            yamlDateParser.parse(created),
            edited?.let { yamlDateParser.parse(it) }
    )
}