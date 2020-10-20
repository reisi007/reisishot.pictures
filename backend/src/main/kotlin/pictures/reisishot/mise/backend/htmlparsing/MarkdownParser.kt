package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.commons.filenameWithoutExtension
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
import java.io.StringReader
import java.nio.file.Files

object MarkdownParser {
    private val extensions = listOf(
            AutolinkExtension.create(),
            TablesExtension.create(),
            TocExtension.create(),
            EmojiExtension.create(),
            AnchorLinkExtension.create(),
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
                    .map { it.processFrontMatter(configuration, cache, targetPath, yamlExtractor.data, galleryGenerator) }
                    .forEach { it(this) }
        }
        val htmlInput = Files.newBufferedReader(sourceFile).use { reader ->
            val parseReader = markdownParser.parseReader(reader)
            yamlExtractor.visit(parseReader)
            StringReader(
                    htmlRenderer.render(
                            parseReader
                    )
            )
        }
        return headManipulator to VelocityApplier.runVelocity(
                htmlInput,
                sourceFile.filenameWithoutExtension,
                targetPath,
                galleryGenerator,
                cache,
                configuration
        )
    }
}