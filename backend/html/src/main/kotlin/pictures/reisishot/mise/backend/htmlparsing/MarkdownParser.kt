package pictures.reisishot.mise.backend.htmlparsing

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader
import java.nio.file.Files


object MarkdownParser {
    private val extensions =
        listOf(
            AutolinkExtension.create(),
            TablesExtension.create(),
            TocExtension.create(),
            EmojiExtension.create(),
            YamlFrontMatterExtension.create(),
        )

    private val markdownParser by lazy {
        Parser.builder()
            .extensions(extensions)
            .apply {
                set(Parser.SPACE_IN_LINK_ELEMENTS, true)
                set(Parser.SPACE_IN_LINK_URLS, true)
            }
            .build()
    }
    private val htmlRenderer by lazy {
        HtmlRenderer
            .builder()
            .extensions(extensions)
            .build()
    }

    fun processMarkdown2Html(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMinimalInfo: IPageMinimalInfo,
        vararg metaDataConsumers: PageGeneratorExtension
    ): Triple<Yaml, HEAD.() -> Unit, String> {
        val readerCreator: () -> BufferedReader =
            { Files.newBufferedReader(pageMinimalInfo.sourcePath, Charsets.UTF_8) }
        return processMarkdown2Html(
            configuration,
            cache,
            pageMinimalInfo,
            readerCreator,
            *metaDataConsumers
        )
    }

    fun processMarkdown2Html(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMinimalInfo: IPageMinimalInfo,
        readerProvider: () -> BufferedReader,
        vararg metaDataConsumers: PageGeneratorExtension
    ): Triple<Yaml, HEAD.() -> Unit, String> {
        val yamlExtractor = AbstractYamlFrontMatterVisitor()
        readerProvider().use {
            extractFrontmatter(it, yamlExtractor)
        }

        val yaml: Yaml = yamlExtractor.data
        val headManipulator: HEAD.() -> Unit = {
            metaDataConsumers.asSequence()
                .map { it.processFrontmatter(configuration, cache, pageMinimalInfo, yaml) }
                .forEach { it(this) }
        }

        val metaData = yaml.getPageMetadata()

        val afterVelocity =
            StringReader(readerProvider().use {
                it.velocity(
                    pageMinimalInfo,
                    metaData,
                    configuration,
                    cache
                )
            })
        val finalHtml = afterVelocity.markdown2Html()



        return Triple(yaml, headManipulator, finalHtml)
    }

    private fun Reader.velocity(
        pageMinimalInfo: IPageMinimalInfo,
        pageMetadata: PageMetadata?,
        configuration: WebsiteConfig,
        cache: BuildingCache,
    ) = VelocityApplier.runVelocity(
        this,
        pageMinimalInfo,
        pageMetadata,
        configuration,
        cache
    )

    fun Reader.markdown2Html() = htmlRenderer.render(markdownParser.parseReader(this))

    fun extractFrontmatter(fileContents: String, target: AbstractYamlFrontMatterVisitor) =
        extractFrontmatter(BufferedReader(StringReader(fileContents)), target)

    private fun extractFrontmatter(fileContents: BufferedReader, target: AbstractYamlFrontMatterVisitor) {
        val content = buildString {
            var line: String? = fileContents.readLine()
            if (line == null || !line.startsWith("---"))
                return@buildString
            append(line)
            do {
                line = fileContents.readLine()
                if (line == null)
                    return@buildString
                append("\n")
                append(line)
            } while (line != null && !line.startsWith("---"))
        }
        val parseReader = markdownParser.parse(content)
        target.visit(parseReader)
    }
}
