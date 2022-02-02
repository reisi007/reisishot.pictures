package pictures.reisishot.mise.backend.htmlparsing

import assertk.assertThat
import assertk.assertions.isEqualTo
import at.reisishot.mise.backend.config.*
import at.reisishot.mise.commons.withChild
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.SourcePath
import pictures.reisishot.mise.backend.TargetPath
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.buildHtmlConfig
import pictures.reisishot.mise.backend.html.config.registerAllTemplateObjects
import java.io.StringReader
import java.nio.file.Paths
import java.util.stream.Stream
import org.junit.jupiter.params.provider.Arguments.of as argsOf

internal class MarkdownParserTest {


    @ParameterizedTest
    @MethodSource("html2markdown")
    fun `ensure small markdown fragments (which are known to be problematic) are converted correctly to HTML`(
        md: String,
        expectedHtml: String
    ) {
        val (_, _, outputHtml) = MarkdownParser.processMarkdown2Html(
            createWebsiteConfig(),
            BuildingCache(),
            createPageMinimalInfo(),
            StringReader(md)
        )

        assertThat(outputHtml).isEqualTo("${expectedHtml.trim()}\n")

    }

    private fun createPageMinimalInfo(): IPageMinimalInfo {
        val cur = Paths.get(".")
        return object : IPageMinimalInfo {
            override val sourcePath: SourcePath = cur withChild "source.md"
            override val targetPath: TargetPath = cur withChild "target.html"
            override val title: String = "Test 123"

        }
    }

    //TODO create test fixture -> a lot of tests will not care about anything except of the lambda at the end
    private fun createWebsiteConfig() = buildWebsiteConfig(
        PathInformation(),
        GeneralWebsiteInformation(
            "Test", "Long test", "https://example.com"
        ),
        MiseConfig(),
    ) {
        buildHtmlConfig {
            registerAllTemplateObjects(
                "obj" to { _, _, _ -> TestTemplateObject() }
            )
        }
    }

    @Suppress("unused")
    class TestTemplateObject : TemplateObject {

        fun prefixHello(name: String) = "Hello $name"

        fun link2Page(name: String) = "https://example.com/$name"
    }

    companion object {
        @JvmStatic
        private fun html2markdown(): Stream<Arguments> = Stream.of(
            argsOf(
                """
               # Test 
            """.trimIndent(),
                """<h1 id="test">Test</h1>"""
            ),
            argsOf(
                """
                    ${'$'}obj.prefixHello("World")
                """.trimIndent(),
                "<p>Hello World</p>"
            ),
            argsOf(
                """
                <a href='${'$'}obj.link2Page("test")' target="_blank">Example test</a>
            """.trimIndent(),
                """<p><a href='https://example.com/test' target="_blank">Example test</a></p>"""
            )
        )
    }
}
