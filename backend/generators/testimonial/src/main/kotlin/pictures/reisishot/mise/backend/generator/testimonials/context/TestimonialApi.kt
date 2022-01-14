package pictures.reisishot.mise.backend.generator.testimonials.context

import TestimonialMode
import appendTestimonials
import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import kotlinx.html.div
import pictures.reisishot.mise.backend.ImageFetcher
import pictures.reisishot.mise.backend.ImageSize
import pictures.reisishot.mise.backend.generator.testimonials.Testimonial
import pictures.reisishot.mise.backend.generator.testimonials.TestimonialLoader
import pictures.reisishot.mise.backend.html.appendUnformattedHtml
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import renderTestimonialStatistics

@WebsiteConfigBuilderDsl
fun createTestimonialApi(
    testimonialLoader: TestimonialLoader,
    imageSizes: Array<out ImageSize>,
    fallback: ImageSize,
    imageFetcher: ImageFetcher
): Pair<String, VelocityTemplateObjectCreator> =
    "testimonials" to { _, websiteConfig, _ ->
        TestimonialApi(imageSizes, fallback, imageFetcher, websiteConfig, testimonialLoader)
    }

class TestimonialApi(
    private val imageSizes: Array<out ImageSize>,
    private val fallback: ImageSize,
    private val galleryGenerator: ImageFetcher,
    private val websiteConfig: WebsiteConfig,
    private val testimonialLoader: TestimonialLoader
) : TemplateObject {
    private val testimonials: Map<String, Testimonial>
        get() = testimonialLoader.load()

    @SuppressWarnings("unused")
    @JvmOverloads
    fun insert(name: String, mode: TestimonialMode = TestimonialMode.DEFAULT) = buildString {
        val testimonialsToDisplay = testimonials.getValue(name)
        appendUnformattedHtml().div {
            appendTestimonials(
                imageSizes,
                fallback,
                websiteConfig,
                galleryGenerator,
                mode,
                false,
                "",// Unused
                testimonialsToDisplay,
            )
        }
    }

    @SuppressWarnings("unused")
    fun insertMultiple(serviceType: String, vararg testimonialTypes: String) = buildString {
        val testimonialsToDisplay = computeMatchingTestimonials(testimonialTypes)
        if (testimonialsToDisplay.isEmpty())
            return@buildString
        appendUnformattedHtml().div {
            appendTestimonials(
                imageSizes,
                fallback,
                websiteConfig,
                galleryGenerator,
                TestimonialMode.DEFAULT,
                true,
                serviceType,
                *testimonialsToDisplay
            )
        }

    }

    @SuppressWarnings("unused")
    fun insertStatistics(serviceType: String, vararg testimonialTypes: String) =
        buildString {
            val testimonialsToDisplay = computeMatchingTestimonials(testimonialTypes)
            if (testimonialsToDisplay.isEmpty())
                return@buildString
            appendUnformattedHtml().div {
                renderTestimonialStatistics(
                    serviceType,
                    websiteConfig.websiteInformation.shortTitle,
                    testimonialsToDisplay
                )
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

}
