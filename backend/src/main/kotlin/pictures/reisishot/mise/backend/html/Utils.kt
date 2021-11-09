package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import kotlinx.html.impl.DelegatingMap
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.df_dd_MM_YYYY
import pictures.reisishot.mise.backend.df_yyyy_MM_dd
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.CategoryInformation
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.getOrThrow
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.pages.PageMetadata
import pictures.reisishot.mise.backend.generator.pages.minimalistic.TargetPath
import pictures.reisishot.mise.backend.generator.testimonials.Testimonial
import pictures.reisishot.mise.backend.loop
import java.util.*
import kotlin.math.roundToInt


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

@HtmlTagMarker
fun HtmlBlockTag.insertImageGallery(
    galleryName: String,
    configuration: WebsiteConfiguration,
    imageInformation: List<ImageInformation>
) = with(imageInformation) {
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

fun HtmlBlockTag.insertImageGallery(
    galleryName: String,
    configuration: WebsiteConfiguration,
    vararg imageInformation: ImageInformation
) = insertImageGallery(galleryName, configuration, listOf(*imageInformation))

internal fun HtmlBlockTag.insertLazyPicture(
    curImageInfo: ImageInformation,
    configuration: WebsiteConfiguration,
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
            ImageSize.values().forEachIndexed { idx, curSize ->
                val thumbnailSize = curImageInfo.thumbnailSizes.getValue(curSize)
                attributes["data-$idx"] = """{
                    |"jpg":"${curImageInfo.getJpgUrl(configuration, curSize)}",
                    |"webp":"${curImageInfo.getWebPUrl(configuration, curSize)}",
                    |"w":${thumbnailSize.width},
                    |"h":${thumbnailSize.height}
                    |}""".trimMargin()
            }
            attributes["data-sizes"] = ImageSize.values().size.toString()
            noScript {
                img(curImageInfo.title, curImageInfo.getJpgUrl(configuration, ImageSize.LARGEST))
            }
        }
    }
}

private fun ImageInformation.getJpgUrl(configuration: WebsiteConfiguration, imageSize: ImageSize): String {
    return getUrl(configuration).substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".jpg"
}

private fun ImageInformation.getWebPUrl(configuration: WebsiteConfiguration, imageSize: ImageSize): String {
    return getUrl(configuration).substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".webp"
}

internal fun HtmlBlockTag.insertSubcategoryThumbnail(
    categoryInformation: CategoryInformation,
    imageInformation: ImageInformation,
    configuration: WebsiteConfiguration
) {
    a("/gallery/categories/${categoryInformation.urlFragment}", classes = "card") {
        div("card-img-top") {
            insertLazyPicture(imageInformation, configuration)
        }
        div("card-body") {
            h4("card-title") {
                text(categoryInformation.displayName)
            }
        }
    }
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.smallButtonLink(
    text: String,
    href: String,
    target: String = "_blank"
) =
    a(href, target, classes = "btn btn-primary btn-sm active") {
        attributes["role"] = "button"
        text(text)
    }

internal var DelegatingMap.value
    get() = this["value"]
    set(value) {
        if (value == null)
            this.remove("value")
        else
            this["value"] = value
    }

internal var DelegatingMap.itemprop
    get() = this["itemprop"]
    set(value) {
        if (value == null)
            this.remove("itemprop")
        else
            this["itemprop"] = value
    }

internal var DelegatingMap.itemscope
    get() = this["itemscope"]
    set(value) {
        if (value == null)
            this.remove("itemscope")
        else
            this["itemscope"] = value
    }

internal var DelegatingMap.itemtype
    get() = this["itemtype"]
    set(value) {
        if (value == null)
            this.remove("itemtype")
        else
            this["itemtype"] = value
    }

object Itemtypes {
    const val ARTICLE = "http://schema.org/Article"
}

internal var DelegatingMap.content
    get() = this["content"]
    set(value) {
        if (value == null)
            this.remove("content")
        else
            this["content"] = value
    }

@HtmlTagMarker
fun DIV.renderTestimonial(
    websiteConfiguration: WebsiteConfiguration,
    targetPath: TargetPath,
    galleryGenerator: AbstractGalleryGenerator,
    mode: TestimonialMode,
    testimonial: Testimonial
) {
    div("card border-black") {
        when (mode) {
            TestimonialMode.BIG -> {
            }
            else -> {
                classes = classes + "col-12" + "col-lg-5"
            }
        }
        attributes.itemprop = "review"
        attributes.itemprop = ""
        attributes.itemtype = "https://schema.org/Review"
        with(galleryGenerator.cache) {
            renderTestimonialVisual(this@div, testimonial, this, targetPath, websiteConfiguration)
        }
        div("card-body") {
            h5("card-title") {
                val rating: Int? = testimonial.rating
                if (rating != null) {

                    span {
                        attributes.itemprop = "reviewRating"
                        attributes.itemscope = ""
                        attributes.itemtype = "https://schema.org/Rating"
                        minMaxRatingMeta()

                        renderRating(rating)
                        text(" ")
                    }
                }
                span {
                    attributes.itemprop = "author"
                    attributes.itemtype = "https://schema.org/Person"
                    text(testimonial.name)
                }
                br()
                small("text-muted") {
                    attributes.itemprop = "datePublished"
                    attributes.content = testimonial.isoDateString
                    text(testimonial.formattedDate)
                }
            }
            if (testimonial.html != null)
                div("card-text") {
                    attributes.itemprop = "reviewBody"
                    raw(testimonial.html)
                }
        }
    }
}

private fun renderTestimonialVisual(
    div: HtmlBlockTag,
    testimonial: Testimonial,
    cache: AbstractGalleryGenerator.Cache,
    targetPath: TargetPath,
    websiteConfiguration: WebsiteConfiguration
) = with(div) {
    if (testimonial.video != null) {
        insertYoutube(testimonial.video, 4, 5, "card-img-top")
    } else if (testimonial.image != null) {
        val curImageInfo = cache.imageInformationData.getOrThrow(testimonial.image, targetPath)
        insertLazyPicture(curImageInfo, websiteConfiguration, "card-img-top")
    } else if (testimonial.images != null) {
        renderCarousel(
            "test-" + testimonial.id,
            5000,
            testimonial.images.toTypedArray(),
            cache,
            targetPath,
            websiteConfiguration
        )
    }
}

fun HtmlBlockTag.renderCarousel(
    id: String,
    changeMs: Int,
    filename: Array<out String>,
    cache: AbstractGalleryGenerator.Cache,
    targetPath: TargetPath,
    websiteConfiguration: WebsiteConfiguration
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

                    with(cache) {
                        insertLazyPicture(
                            imageInformationData.getOrThrow(filename, targetPath),
                            websiteConfiguration,
                            "d-block w-100"
                        )
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

fun HtmlBlockTag.renderRating(rating: Int, starSize: String = "sm") {
    renderRating(rating.toDouble(), starSize)
}

fun HtmlBlockTag.renderRating(rating: Double, starSize: String = "sm") {
    val roundedRating = (rating / 10).roundToInt()
    val stars = roundedRating / 2
    val halfStar = 2 * stars < roundedRating
    val emptyStars = 5 - stars - (if (halfStar) 1 else 0)

    span("align-base") {
        attributes.itemprop = "ratingValue"
        attributes.content = rating.toString()

        loop(stars) {
            insertIcon(ReisishotIcons.STAR_FULL, starSize)
        }
        if (halfStar)
            insertIcon(ReisishotIcons.STAR_HALF, starSize)

        loop(emptyStars) {
            insertIcon(ReisishotIcons.STAR_NONE, starSize)
        }
    }
}

enum class TestimonialMode {
    DEFAULT,
    BIG
}

@HtmlTagMarker
fun HtmlBlockTag.appendTestimonials(
    websiteConfiguration: WebsiteConfiguration,
    targetPath: TargetPath,
    galleryGenerator: AbstractGalleryGenerator,
    mode: TestimonialMode,
    displayStatistics: Boolean,
    type: String,
    vararg testimonialsToDisplay: Testimonial
) {
    if (displayStatistics && testimonialsToDisplay.isNotEmpty()) {
        div("text-center") {
            if (testimonialsToDisplay.find { it.rating != null } != null)
                text("Durchschnittliche Bewertung:")
            renderTestimonialStatistics(type, websiteConfiguration, testimonialsToDisplay)
        }
    }
    div {
        when (mode) {
            TestimonialMode.BIG -> {
            }
            else -> {
                classes = classes + "container-flex" + "reviews"
            }
        }

        val sortTestimonials =
            compareBy<Testimonial>(
                { it.image == null && it.video == null && it.images == null },
                { it.html == null }
            )
                .thenByDescending { it.isoDateString }
                .thenByDescending { it.rating ?: -1 }
                .thenByDescending { it.html?.length ?: -1 }


        testimonialsToDisplay.asSequence()
            .sortedWith(sortTestimonials)
            .forEach { testimonial ->
                renderTestimonial(websiteConfiguration, targetPath, galleryGenerator, mode, testimonial)
            }

    }
}

internal fun HtmlBlockTag.renderTestimonialStatistics(
    serviceName: String,
    websiteConfiguration: WebsiteConfiguration,
    testimonialsToDisplay: Array<out Testimonial>
) {
    val statisticsData = testimonialsToDisplay.asSequence()
        .map { it.rating?.toDouble() }
        .filterNotNull()
        .statistics()
    if (statisticsData != null && statisticsData.cnt > 0) {
        span("lh-lg text-center align-middle") {
            attributes.itemprop = "aggregateRating"
            attributes.itemscope = ""
            attributes.itemtype = "https://schema.org/AggregateRating"
            renderRating(statisticsData.avg, "2x")
            minMaxRatingMeta()
            meta {
                attributes.itemprop = "name"
                attributes.itemscope = ""
                attributes.value = serviceName
            }
            span {
                attributes.itemprop = "itemReviewed"
                attributes.itemscope = ""
                attributes.itemtype = "https://schema.org/LocalBusiness"

                meta {
                    attributes.itemprop = "name"
                    attributes.itemscope = ""
                    attributes.value = websiteConfiguration.shortTitle
                }

            }
            span("align-text-bottom") {
                attributes.itemprop = "ratingCount"
                attributes.content = statisticsData.cnt.toString()
                text(" (${statisticsData.cnt})")
            }
        }
    }
}

internal fun FlowOrPhrasingContent.minMaxRatingMeta() {
    meta {
        attributes.itemprop = "worstRating"
        attributes.content = "0"
    }
    meta {
        attributes.itemprop = "bestRating"
        attributes.content = "100"
    }
}

data class StatisticsData(val avg: Double, val cnt: Long)

private fun Sequence<Double>.statistics(): StatisticsData? {
    var cnt: Long = 0
    var sum: Double = 0.toDouble()

    forEach {
        cnt++
        sum += it
    }
    if (cnt == 0L)
        return null
    return StatisticsData(sum / cnt, cnt)
}

@HtmlTagMarker
internal fun FlowOrPhrasingContent.metadata(metadata: PageMetadata) {
    span("badge bg-light text-secondary text-pre-wrap") {
        metadata.edited?.let {
            time {
                attributes.itemprop = "dateModified"
                dateTime = df_yyyy_MM_dd.format(it)

                span("font-weight-normal") {
                    text("Zuletzt aktualisiert am: ")
                }
                text(it)
            }
            text("  –  ")
        }
        time {
            attributes.itemprop = "datePublished"
            dateTime = df_yyyy_MM_dd.format(metadata.created)
            span("font-weight-normal") {
                text("Veröffentlicht am: ")
            }
            text(metadata.created)
        }
    }
}

@HtmlTagMarker
internal fun Tag.text(date: Date) {
    text(df_dd_MM_YYYY.format(date))
}

@HtmlTagMarker
internal fun HtmlBlockTag.insertYoutube(codeOrLinkFragment: String, w: Int, h: Int, vararg additionalClasses: String) {
    p("ratio") {
        classes = classes + ("ratio-${w}x$h")
        if (additionalClasses.isNotEmpty())
            classes = classes + additionalClasses

        iframe(classes = "lazy") {
            attributes["data-src"] =
                if (codeOrLinkFragment.startsWith("http")) codeOrLinkFragment else "https://www.youtube-nocookie.com/embed/$codeOrLinkFragment"
            attributes["allow"] =
                "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            attributes["allowfullscreen"] = ""
        }
    }
}

@HtmlTagMarker
fun DIV.insertWartelisteInfo() {
    p("d-inline-block") { text("Wenn du dich für ein Shooting anmelden möchtest trage dich einfach direkt in die Warteliste ein:") }
    a("https://service.reisishot.pictures/waitlist", "_blank", "pl-2 btn btn-primary") {
        text("Zur Anmeldung gehen ")
        insertIcon(ReisishotIcons.LINK, "xs", "text-white")
    }
}

fun <T : Appendable> T.appendUnformattedHtml() = appendHTML(false, true)
