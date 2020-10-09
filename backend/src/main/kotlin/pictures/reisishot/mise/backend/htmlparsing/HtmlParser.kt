package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.commons.exists
import at.reisishot.mise.commons.filenameWithoutExtension
import at.reisishot.mise.commons.useBufferedReader
import at.reisishot.mise.commons.withChild
import kotlinx.html.HEAD
import org.yaml.snakeyaml.Yaml
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
import pictures.reisishot.mise.backend.html.raw
import java.io.Reader
import java.nio.file.Files

object HtmlParser {
    private val yamlParser by lazy { Yaml() }

    fun parse(
            websiteConfiguration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            soureFile: SourcePath,
            targetFile: TargetPath,
            galleryGenerator: AbstractGalleryGenerator,
            vararg metaDataConsumers: YamlMetaDataConsumer
    ): Pair<HEAD.() -> Unit, String> = Files.newBufferedReader(soureFile).use { reader: Reader ->
        val processHeadFile: HEAD.() -> Unit = {
            val headFile = soureFile.parent withChild soureFile.filenameWithoutExtension + ".head"
            if (headFile.exists()) {
                val headContent = headFile.useBufferedReader { it.readText() }
                raw(headContent)
            }
            val yamlFile = soureFile.parent withChild soureFile.filenameWithoutExtension + ".yaml"
            if (yamlFile.exists()) {
                yamlFile.useBufferedReader {
                    @Suppress("UNCHECKED_CAST")
                    (yamlParser.loadAs(it, Map::class.java) as? Map<String, Any>)?.let { data ->
                        val headManipulator: HEAD.() -> Unit = {
                            metaDataConsumers.asSequence()
                                    .map { it.processFrontMatter(websiteConfiguration, buildingCache, targetFile, data, galleryGenerator) }
                                    .forEach { it(this) }
                        }
                        headManipulator(this)
                    }
                }
            }
        }

        val body = VelocityApplier.runVelocity(reader, soureFile.filenameWithoutExtension, targetFile, galleryGenerator, buildingCache, websiteConfiguration)

        return@use processHeadFile to body
    }
}