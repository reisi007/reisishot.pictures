package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.commons.FilenameWithoutExtension
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.app.VelocityEngine
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.writeToString
import java.io.Reader
import java.nio.file.Path

object VelocityApplier {
    private val compressHtmlRegex = """[\s\n\r]{2,}""".toRegex()
    private val velocity by lazy {
        Velocity.init()
        return@lazy VelocityEngine()
    }

    private fun VelocityContext.withTemplateApi(
            targetPath: Path,
            galleryGenerator: AbstractGalleryGenerator,
            buildingCache: BuildingCache,
            websiteConfiguration: WebsiteConfiguration
    ) = apply {
        put("please", TemplateApi(targetPath, galleryGenerator, buildingCache, websiteConfiguration))
    }

    fun runVelocity(
            reader: Reader,
            srcPath: FilenameWithoutExtension,
            targetPath: Path,
            galleryGenerator: AbstractGalleryGenerator,
            buildingCache: BuildingCache,
            websiteConfiguration: WebsiteConfiguration
    ) = writeToString {
        try {
            velocity.evaluate(
                    VelocityContext().withTemplateApi(targetPath, galleryGenerator, buildingCache, websiteConfiguration),
                    it,
                    "HtmlParser",
                    reader
            )
        } catch (e: Exception) {
            throw IllegalStateException("Could not parse \"$srcPath!\"", e)
        }
    }
            .replace(compressHtmlRegex, " ")
}