import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.SourcePath
import pictures.reisishot.mise.backend.TargetPath
import pictures.reisishot.mise.backend.generator.pages.overview.extract
import pictures.reisishot.mise.backend.htmlparsing.buildYaml
import java.nio.file.Paths

class OverviewPageGeneratorTest {

    @Test
    fun `Sorting should work with creation Date`() {
        val elements = sequenceOf(
            createYaml("2", 2020_02_01, 2020_05_01), // Second as the edited date is bigger than the creation date of 3
            createYaml("3", 2020_02_01),
            createYaml(
                "1",
                edited = 2020_05_01
            ), // No created date, but an edited date -> Treat edited as created -> First as the creation date of 2 is further in the past
        )

        val pageMinimalInfo = object : IPageMinimalInfo {
            override val sourcePath: SourcePath = Paths.get(".", "in")
            override val targetPath: TargetPath = Paths.get(".", "out")
            override val title: String = "Unused..."
        }

        val sorted = elements.map { it.extract(pageMinimalInfo, emptyMap()) ?: error("must not be null") }
            .sortedByDescending { it.order }
            .map { it.title }
            .toList()

        assertThat(sorted).containsExactly("1", "2", "3")
    }

    private fun createYaml(title: String, created: Int? = null, edited: Int? = null) = buildYaml {
        "group" to "test"
        "picture" to "some"
        "title" to title
        if (created != null)
            "created" to created.toString()
        if (edited != null)
            "updated" to edited.toString()
    }
}
