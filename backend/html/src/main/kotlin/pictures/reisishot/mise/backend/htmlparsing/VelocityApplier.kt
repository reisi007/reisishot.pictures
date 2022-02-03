package pictures.reisishot.mise.backend.htmlparsing

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.app.VelocityEngine
import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.html.config.htmlConfig
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import java.util.*

object VelocityApplier {
    private val velocity by lazy {
        val p = Properties()
        p["parser.class"] = "pictures.reisishot.velocity.runtime.parser.custom.MdCompatibleParser"
        Velocity.init(p)
        return@lazy VelocityEngine(p)
    }

    fun runVelocity(
        reader: Reader,
        pageMinimalInfo: IPageMinimalInfo,
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
            throw IllegalStateException("Could not parse file \"${pageMinimalInfo.sourcePath})!\"", e)
        }
    }
}

private fun writeToString(action: (Writer) -> Unit) = StringWriter().apply(action).toString()
