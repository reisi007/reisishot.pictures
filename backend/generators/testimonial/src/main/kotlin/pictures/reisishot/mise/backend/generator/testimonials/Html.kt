import at.reisishot.mise.backend.config.WebsiteConfig
import kotlinx.html.*
import pictures.reisishot.mise.backend.ImageFetcher
import pictures.reisishot.mise.backend.ImageSize
import pictures.reisishot.mise.backend.generator.testimonials.Testimonial
import pictures.reisishot.mise.backend.html.*
import kotlin.math.roundToInt

@HtmlTagMarker
fun DIV.renderTestimonial(
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    websiteConfig: WebsiteConfig,
    galleryCache: ImageFetcher,
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

        renderTestimonialVisual(imageSizes, fallback, this, testimonial, galleryCache, websiteConfig)

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
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    div: HtmlBlockTag,
    testimonial: Testimonial,
    cache: ImageFetcher,
    websiteConfig: WebsiteConfig
) = with(div) {
    if (testimonial.video != null) {
        insertYoutube(testimonial.video, 4, 5, "card-img-top")
    } else if (testimonial.image != null) {
        val curImageInfo = cache(testimonial.image)
        insertLazyPicture(imageSizes, fallback, curImageInfo, websiteConfig, "card-img-top")
    } else if (testimonial.images != null) {
        renderCarousel(
            imageSizes,
            fallback,
            "test-" + testimonial.id,
            5000,
            testimonial.images.toTypedArray(),
            cache,
            websiteConfig
        )
    }
}

@HtmlTagMarker
fun HtmlBlockTag.appendTestimonials(
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    websiteConfig: WebsiteConfig,
    galleryGenerator: ImageFetcher,
    mode: TestimonialMode,
    displayStatistics: Boolean,
    type: String,
    vararg testimonialsToDisplay: Testimonial
) {
    if (displayStatistics && testimonialsToDisplay.isNotEmpty()) {
        div("text-center") {
            if (testimonialsToDisplay.find { it.rating != null } != null)
                text("Durchschnittliche Bewertung:")
            renderTestimonialStatistics(type, websiteConfig.websiteInformation.shortTitle, testimonialsToDisplay)
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
        1
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
                renderTestimonial(
                    imageSizes,
                    fallback,
                    websiteConfig,
                    galleryGenerator,
                    mode,
                    testimonial
                )
            }

    }
}

internal fun HtmlBlockTag.renderTestimonialStatistics(
    serviceName: String,
    shortTitle: String,
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
                    attributes.value = shortTitle
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

enum class TestimonialMode {
    DEFAULT,
    BIG
}

fun FlowOrPhrasingContent.minMaxRatingMeta() {
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

        for (star in 1..stars) {
            insertIcon(ReisishotIcons.STAR_FULL, starSize)
        }
        if (halfStar)
            insertIcon(ReisishotIcons.STAR_HALF, starSize)

        for (star in 1..emptyStars) {
            insertIcon(ReisishotIcons.STAR_NONE, starSize)
        }
    }
}
