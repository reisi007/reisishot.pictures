package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.backend.config.BuildingCache
import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.commons.FilenameWithoutExtension
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.app.VelocityEngine
import pictures.reisishot.mise.backend.html.config.htmlConfig
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import java.util.*

object VelocityApplier {
    private val velocity by lazy {
        val p = Properties()
        p["parser.class"] = "at.reisishot.velocity.runtime.parser.custom.MdCompatibleParser"
        Velocity.init(p)
        return@lazy VelocityEngine(p)
    }

    fun runVelocity(
        reader: Reader,
        srcPath: FilenameWithoutExtension,
        pageMetadata: PageMetadata?,
        websiteConfig: WebsiteConfig,
        cache: BuildingCache
    ) = writeToString {
        try {
            val velocityContext = VelocityContext().apply {
                websiteConfig.htmlConfig.templateObjects
                    .forEach { (key, creator) ->
                        put(key, creator(pageMetadata, websiteConfig, cache))
                    }

            }
            velocity.evaluate(
                velocityContext,
                it,
                "HtmlParser",
                reader
            )
        } catch (e: Exception) {
            throw IllegalStateException("Could not parse \"$srcPath!\"", e)
        }
    }
}

private fun writeToString(action: (Writer) -> Unit) = StringWriter().apply(action).toString()
