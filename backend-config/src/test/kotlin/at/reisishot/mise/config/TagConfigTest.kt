package at.reisishot.mise.config

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.tags.TagConfig
import pictures.reisishot.mise.backend.config.tags.additionalTags
import pictures.reisishot.mise.backend.config.tags.buildTagConfig
import pictures.reisishot.mise.backend.config.tags.computeTags

class TagConfigTest {

    @Test
    fun `transitive tag dependencies should work`() {
        val config = buildTagConfig {
            additionalTags {
                "B" withTags "C"
                "A" withTags "B"
            }
        }

        val internalImageInformation = buildImageInformation("A")

        config.computeTags(internalImageInformation)

        val tags = internalImageInformation.tags
        assertThat(tags.map { it.name }).containsExactly("A", "B", "C")
    }

    private fun TagConfig.computeTags(image: ImageInformation): Unit = computeTags(listOf(image))
}
