package pictures.reisishot.mise.backend.config

import assertk.assertThat
import assertk.assertions.containsExactly
import at.reisishot.mise.commons.concurrentSetOf

import org.junit.jupiter.api.Test
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.TagInformation

class TagConfigTest {

    @Test
    fun `transitive tag dependencies should work`() {
        val config = buildTagConfig {
            additionalTags {
                "B" withTags "C"
                "A" withTags "B"
            }
        }

        val internalImageInformation = buildInternalImageInformation("A")

        config.computeTags(internalImageInformation)

        val tags = internalImageInformation.tags
        assertThat(tags.map { it.name }).containsExactly("A", "B", "C")
    }

    private fun TagConfig.computeTags(image: InternalImageInformation): Unit = computeTags(listOf(image))

    private fun buildInternalImageInformation(vararg tags: String): InternalImageInformation {
        return InternalImageInformation(
            "---",
            mutableMapOf(),
            "---",
            "---",
            tags.asSequence()
                .map { TagInformation(it) }
                .toCollection(concurrentSetOf()),
            mutableMapOf()
        )
    }
}
