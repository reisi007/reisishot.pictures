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
import java.util.*

object VelocityApplier {
    private val velocity by lazy {
        val p = Properties()
        p["parser.class"] = "at.reisishot.velocity.runtime.parser.custom.MdCompatibleParser"
        Velocity.init(p)
        return@lazy VelocityEngine(p)
    }

    private fun VelocityContext.withTemplateApi(
            pageMetadata: PageMetadata?,
            targetPath: Path,
            galleryGenerator: AbstractGalleryGenerator,
            buildingCache: BuildingCache,
            websiteConfiguration: WebsiteConfiguration
    ) = apply {
        put("please", TemplateApi(pageMetadata, targetPath, galleryGenerator, buildingCache, websiteConfiguration))
    }

    fun runVelocity(
            reader: Reader,
            srcPath: FilenameWithoutExtension,
            targetPath: Path,
            galleryGenerator: AbstractGalleryGenerator,
            buildingCache: BuildingCache,
            websiteConfiguration: WebsiteConfiguration,
            pageMetadata: PageMetadata?
    ) = writeToString {
        try {
            velocity.evaluate(
                    VelocityContext().withTemplateApi(pageMetadata, targetPath, galleryGenerator, buildingCache, websiteConfiguration),
                    it,
                    "HtmlParser",
                    reader
            )
        } catch (e: Exception) {
            throw IllegalStateException("Could not parse \"$srcPath!\"", e)
        }
    }
}