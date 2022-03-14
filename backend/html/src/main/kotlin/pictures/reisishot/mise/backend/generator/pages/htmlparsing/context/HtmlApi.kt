@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package pictures.reisishot.mise.backend.generator.pages.htmlparsing.context

import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.role
import kotlinx.html.span
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import pictures.reisishot.mise.backend.html.appendUnformattedHtml
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import pictures.reisishot.mise.backend.html.insertWartelisteInfo
import pictures.reisishot.mise.backend.html.insertYoutube
import pictures.reisishot.mise.backend.html.metadata
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata

@WebsiteConfigBuilderDsl
fun createHtmlApi(): Pair<String, VelocityTemplateObjectCreator> =
    "page" to { pageMetadata, websiteConfig, cache ->
        HtmlApi(pageMetadata, cache, websiteConfig)
    }

internal class HtmlApi(
    private val pageMetadata: PageMetadata?,
    private val cache: BuildingCache,
    private val websiteConfig: WebsiteConfig,
) : TemplateObject {

    fun insertLink(type: String, key: String): String = cache.getLinkcacheEntryFor(websiteConfig, type, key)

    fun insertLink(linktext: String, type: String, key: String): String = buildString {
        appendUnformattedHtml().a(insertLink(type, key)) {
            text(linktext)
        }
    }

    @JvmOverloads
    fun toKontaktformular(text: String = "Zum Kontaktformular") = buildString {
        appendUnformattedHtml().div("center") {
            a("#footer", classes = "btn btn-primary") {
                text(text)
            }
        }
    }

    fun addMeta(): String {
        val metadata = pageMetadata ?: throw IllegalStateException("No page metadata specified!")
        return buildString {
            appendUnformattedHtml().div {
                metadata(metadata)
            }
        }
    }

    fun insertTextCarousel(changeMs: Int, vararg text: String) =
        insertTextCarousel("testimonials", changeMs, *text)

    fun insertYoutube(codeOrLinkFragment: String, w: Int, h: Int) = buildString {
        appendUnformattedHtml().div {
            insertYoutube(codeOrLinkFragment, w, h)
        }
    }

    fun insertTextCarousel(id: String, changeMs: Int, vararg text: String) = buildString {
        appendUnformattedHtml().div("carousel slide") {
            this.id = id
            attributes["data-bs-interval"] = changeMs.toString()
            attributes["data-bs-ride"] = "carousel"
            div("carousel-inner") {
                text.forEachIndexed { idx, cur ->
                    div {
                        classes = classes + "carousel-item"
                        if (idx == 0)
                            classes = classes + "active"

                        p {
                            span {
                                text("„$cur“")
                            }
                        }
                    }
                }
            }

            a("#$id", classes = "carousel-control-prev") {
                role = "button"
                attributes["data-bs-slide"] = "prev"
                span("carousel-control-prev-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("visually-hidden") { text("Vorheriger Text") }
            }

            a("#$id", classes = "carousel-control-next") {
                role = "button"
                attributes["data-bs-slide"] = "next"
                span("carousel-control-next-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("visually-hidden") { text("Nächster Text") }
            }
        }
    }

    fun insertVideoCarousel(id: String, changeMs: Int, vararg ytCodes: String) = buildString {
        appendUnformattedHtml().div("carousel slide") {
            this.id = id
            attributes["data-bs-interval"] = changeMs.toString()
            attributes["data-bs-ride"] = "carousel"
            div("carousel-inner") {
                ytCodes.forEachIndexed { idx, ytCode ->
                    div {
                        classes = classes + "carousel-item"
                        if (idx == 0)
                            classes = classes + "active"

                        insertYoutube(ytCode, 1, 1)
                    }
                }
            }

            a("#$id", classes = "carousel-control-prev") {
                role = "button"
                attributes["data-bs-slide"] = "prev"
                span("carousel-control-prev-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("visually-hidden") { text("Vorheriges Video") }
            }

            a("#$id", classes = "carousel-control-next") {
                role = "button"
                attributes["data-bs-slide"] = "next"
                span("carousel-control-next-icon") {
                    attributes["aria-hidden"] = "true"
                }
                span("visually-hidden") { text("Nächstes Video") }
            }
        }
    }

    fun insertWartelisteInfo(): String = buildString {
        appendUnformattedHtml().div {
            insertWartelisteInfo()
        }
    }
}
