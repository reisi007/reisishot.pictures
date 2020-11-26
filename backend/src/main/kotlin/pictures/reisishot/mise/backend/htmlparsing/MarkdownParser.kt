package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.commons.filenameWithoutExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
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

    fun parse(configuration: WebsiteConfiguration, cache: BuildingCache, sourceFile: SourcePath, targetPath: TargetPath, galleryGenerator: AbstractGalleryGenerator, vararg metaDataConsumers: YamlMetaDataConsumer): Pair<HEAD.() -> Unit, String> {
        val yamlExtractor = AbstractYamlFrontMatterVisitor()
        val headManipulator: HEAD.() -> Unit = {
            metaDataConsumers.asSequence()
                    .map { it.processFrontmatter(configuration, cache, targetPath, yamlExtractor.data) }
                    .forEach { it(this) }
        }
        val html = Files.newBufferedReader(sourceFile, Charsets.UTF_8)
                .velocity(sourceFile, targetPath, galleryGenerator, cache, configuration)
                .markdown2Html(yamlExtractor)



        return headManipulator to html
    }

    private fun Reader.velocity(sourceFile: SourcePath, targetPath: TargetPath, galleryGenerator: AbstractGalleryGenerator, cache: BuildingCache, configuration: WebsiteConfiguration) = VelocityApplier.runVelocity(
            this,
            sourceFile.filenameWithoutExtension,
            targetPath,
            galleryGenerator,
            cache,
            configuration
    )

    private fun String.velocity(sourceFile: SourcePath, targetPath: TargetPath, galleryGenerator: AbstractGalleryGenerator, cache: BuildingCache, configuration: WebsiteConfiguration) = VelocityApplier.runVelocity(
            StringReader(this),
            sourceFile.filenameWithoutExtension,
            targetPath,
            galleryGenerator,
            cache,
            configuration
    )

    private fun String.markdown2Html(yamlExtractor: AbstractYamlFrontMatterVisitor) =
            htmlRenderer.render(extractFrontmatter(this, yamlExtractor))

    private fun Reader.markdown2Html(yamlExtractor: AbstractYamlFrontMatterVisitor) =
            htmlRenderer.render(extractFrontmatter(this, yamlExtractor))

    fun extractFrontmatter(fileContents: String, target: AbstractYamlFrontMatterVisitor): Document = extractFrontmatter(StringReader(fileContents), target)

    fun extractFrontmatter(fileContents: Reader, target: AbstractYamlFrontMatterVisitor): Document {
        val parseReader = markdownParser.parseReader(fileContents)
        target.visit(parseReader)
        return parseReader
    }
}