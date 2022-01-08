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
import pictures.reisishot.mise.backend.generator.pages.IPageMininmalInfo
import pictures.reisishot.mise.backend.generator.pages.PageGeneratorExtension
import pictures.reisishot.mise.backend.generator.pages.PageMetadata
import pictures.reisishot.mise.backend.generator.pages.getPageMetadata
import pictures.reisishot.mise.backend.generator.pages.minimalistic.SourcePath
import pictures.reisishot.mise.backend.generator.pages.minimalistic.Yaml
import pictures.reisishot.mise.backend.generator.testimonials.TestimonialLoader
import java.io.Reader
import java.io.StringReader
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.lang.reflect.Modifier
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

    private fun Class<*>.setField(fieldName: String, value: Any?) {
        val field = getDeclaredField(fieldName)
        field.isAccessible = true
        val lookup = MethodHandles.privateLookupIn(Field::class.java, MethodHandles.lookup())
        val modifiers = lookup.findVarHandle(Field::class.java, "modifiers", Int::class.javaPrimitiveType)
        modifiers.set(field, field.modifiers and Modifier.FINAL.inv())
        field.set(this, value)
    }

    fun parse(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        testimonialLoader: TestimonialLoader,
        pageMininmalInfo: IPageMininmalInfo,
        galleryGenerator: AbstractGalleryGenerator,
        vararg metaDataConsumers: PageGeneratorExtension
    ): Triple<Yaml, HEAD.() -> Unit, String> {
        val sourceFile = pageMininmalInfo.sourcePath
        val yamlExtractor = AbstractYamlFrontMatterVisitor()
        val headManipulator: HEAD.() -> Unit = {
            metaDataConsumers.asSequence()
                .map { it.processFrontmatter(configuration, cache, pageMininmalInfo, yamlExtractor.data) }
                .forEach { it(this) }
        }

        val parsedMarkdown = Files.newBufferedReader(sourceFile, Charsets.UTF_8)
            .markdown2Html(yamlExtractor)
        val yaml: Yaml = yamlExtractor.data
        val metaData = yaml.getPageMetadata()
        val finalHtml = parsedMarkdown
            .velocity(sourceFile, metaData, pageMininmalInfo, testimonialLoader, galleryGenerator, cache, configuration)
        return Triple(yaml, headManipulator, finalHtml)
    }

    private fun String.velocity(
        sourceFile: SourcePath,
        metadata: PageMetadata?,
        pageMininmalInfo: IPageMininmalInfo,
        testimonialLoader: TestimonialLoader,
        galleryGenerator: AbstractGalleryGenerator,
        cache: BuildingCache,
        configuration: WebsiteConfiguration,
    ) = VelocityApplier.runVelocity(
        StringReader(this),
        sourceFile.filenameWithoutExtension,
        pageMininmalInfo.targetPath,
        galleryGenerator,
        cache,
        configuration,
        testimonialLoader,
        metadata
    )

    fun Reader.markdown2Html(yamlExtractor: AbstractYamlFrontMatterVisitor) =
        htmlRenderer.render(extractFrontmatter(this, yamlExtractor))

    fun extractFrontmatter(fileContents: String, target: AbstractYamlFrontMatterVisitor): Document =
        extractFrontmatter(StringReader(fileContents), target)

    private fun extractFrontmatter(fileContents: Reader, target: AbstractYamlFrontMatterVisitor): Document {
        val parseReader = markdownParser.parseReader(fileContents)
        target.visit(parseReader)
        return parseReader
    }
}
