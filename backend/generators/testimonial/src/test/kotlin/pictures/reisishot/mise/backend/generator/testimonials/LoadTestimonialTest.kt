package pictures.reisishot.mise.backend.generator.testimonials

import assertk.assertThat
import assertk.assertions.containsAll
import at.reisishot.mise.commons.testfixtures.softAssert
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.buildTestWebsiteConfig
import pictures.reisishot.mise.backend.html.config.buildHtmlConfig
import pictures.reisishot.mise.commons.toTypedArray
import kotlin.io.path.createDirectories

class LoadTestimonialTest {

    @Test
    fun `Loading of testimonials works`() {
        val websiteConfig = createWebsiteConfig()

        val ids = (1..2).asSequence().map { it.toString() }.toTypedArray()
        writePath(websiteConfig.paths, *ids)

        val testimonials =
            createAndLoadTestimonials(websiteConfig) { TestimonialLoaderImpl.fromSourceFolder(websiteConfig.paths.sourceFolder) }

        assertThat(testimonials.keys).containsAll(*ids)
    }

    private fun createAndLoadTestimonials(
        websiteConfig: WebsiteConfig,
        testimonialLoader: () -> TestimonialLoader = { createTestimonialLoader(websiteConfig.paths) }
    ): Map<String, Testimonial> {
        return testimonialLoader().load(websiteConfig, BuildingCache())
    }

    @Test
    fun `Make sure that image can be loaded`() {
        val websiteConfig = createWebsiteConfig()
        with(websiteConfig.paths.sourceFolder) {
            createDirectories()
            createTestimonial("1")
        }
        val loadedTestimonials = createAndLoadTestimonials(websiteConfig)

        assertThat(loadedTestimonials.keys).containsAll("1")

        val testimonials = loadedTestimonials.getValue("1")

        softAssert {
            assertThat(testimonials.image).isEqualTo("1")
            sequenceOf(testimonials.images, testimonials.video).forEach {
                assertThat(it).isNull()
            }
        }
    }

    @Test
    fun `Make sure that video can be loaded`() {
        val websiteConfig = createWebsiteConfig()
        val VALUE_VIDEO = "video"
        with(websiteConfig.paths.sourceFolder) {
            createDirectories()
            createTestimonial("1", image = null, video = VALUE_VIDEO)
        }
        val loadedTestimonials = createAndLoadTestimonials(websiteConfig)

        assertThat(loadedTestimonials.keys).containsAll("1")

        val testimonials = loadedTestimonials.getValue("1")

        softAssert {
            assertThat(testimonials.video).isEqualTo(VALUE_VIDEO)
            sequenceOf(testimonials.image, testimonials.images).forEach {
                assertThat(it).isNull()
            }
        }
    }

    @Test
    fun `Make sure that images can be loaded`() {
        val websiteConfig = createWebsiteConfig()
        val VALUE_IMAGES = listOf("1", "2")
        with(websiteConfig.paths.sourceFolder) {
            createDirectories()
            createTestimonial("1", image = null, images = VALUE_IMAGES)
        }
        val loadedTestimonials = createAndLoadTestimonials(websiteConfig)

        assertThat(loadedTestimonials.keys).containsAll("1")

        val testimonials = loadedTestimonials.getValue("1")

        softAssert {
            assertThat(testimonials.images).containsAll(VALUE_IMAGES)
            sequenceOf(testimonials.image, testimonials.video).forEach {
                assertThat(it).isNull()
            }
        }
    }

    private fun createWebsiteConfig() = buildTestWebsiteConfig { buildHtmlConfig { } }
}
