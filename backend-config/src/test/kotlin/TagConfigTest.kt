import assertk.assertThat
import assertk.assertions.containsExactly
import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.ConcurrentSet
import at.reisishot.mise.commons.concurrentSetOf
import at.reisishot.mise.exifdata.ExifdataKey
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.backend.config.*
import pictures.reisishot.mise.backend.config.tags.TagInformation

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

    private fun TagConfig.computeTags(image: ImageInformation): Unit = computeTags(listOf(image))

    private fun buildInternalImageInformation(vararg tags: String): ImageInformation {
        return object : ImageInformation {
            override val categories: ConcurrentSet<CategoryName> = concurrentSetOf()
            override val tags: ConcurrentSet<TagInformation> = concurrentSetOf(tags.map { TagInformation(it) })
            override val exifInformation: Map<ExifdataKey, String> = emptyMap()
        }
    }
}
