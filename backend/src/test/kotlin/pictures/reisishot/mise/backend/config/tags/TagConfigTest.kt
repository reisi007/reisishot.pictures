package pictures.reisishot.mise.backend.config.tags

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.backend.config.ExtImageInformation
import pictures.reisishot.mise.commons.concurrentSetOf
import pictures.reisishot.mise.config.buildImageInformation

class TagConfigTest {

    @Test
    fun `transitive tag dependencies should work`() {
        val config = buildTagConfig {
            additionalTags {
                "B" withTags "C"
                "A" withTags "B"
            }
        }

        val internalImageInformation = buildImageInformation(concurrentSetOf("A"))

        config.computeTags(internalImageInformation)

        val tags = internalImageInformation.tags
        assertThat(tags.map { it.name }).containsExactly("A", "B", "C")
    }

    private fun TagConfig.computeTags(image: ExtImageInformation): Unit = computeTags(listOf(image))
}
