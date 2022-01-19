package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import kotlinx.html.impl.DelegatingMap
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.df_dd_MM_YYYY
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata
import java.util.*

@HtmlTagMarker
fun HTMLTag.raw(content: String): Unit = consumer.onTagContentUnsafe {
    this.raw(content)
}

@HtmlTagMarker
fun FlowContent.divId(divId: String, classes: String? = null, block: DIV.() -> Unit = {}): Unit =
    DIV(attributesMapOf("class", classes), consumer).visit {
        id = divId
        block(this)
    }

@HtmlTagMarker
fun FlowContent.container(block: DIV.() -> Unit = {}) = div("container", block)

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.smallButtonLink(
    text: String,
    href: String,
    target: String = "_blank"
) =
    a(href, target, classes = "btn btn-primary btn-sm active") {
        attributes["role"] = "button"
        text(text)
    }

var DelegatingMap.value
    get() = this["value"]
    set(value) {
        if (value == null)
            this.remove("value")
        else
            this["value"] = value
    }

var DelegatingMap.itemprop
    get() = this["itemprop"]
    set(value) {
        if (value == null)
            this.remove("itemprop")
        else
            this["itemprop"] = value
    }

var DelegatingMap.itemscope
    get() = this["itemscope"]
    set(value) {
        if (value == null)
            this.remove("itemscope")
        else
            this["itemscope"] = value
    }

var DelegatingMap.itemtype
    get() = this["itemtype"]
    set(value) {
        if (value == null)
            this.remove("itemtype")
        else
            this["itemtype"] = value
    }

var DelegatingMap.content
    get() = this["content"]
    set(value) {
        if (value == null)
            this.remove("content")
        else
            this["content"] = value
    }

@HtmlTagMarker
fun HtmlBlockTag.insertYoutube(codeOrLinkFragment: String, w: Int, h: Int, vararg additionalClasses: String) {
    p("ratio") {
        classes = classes + ("ratio-${w}x$h")
        if (additionalClasses.isNotEmpty())
            classes = classes + additionalClasses

        iframe(classes = "lazy") {
            attributes["data-src"] =
                if (codeOrLinkFragment.startsWith("http")) codeOrLinkFragment else "https://www.youtube-nocookie.com/embed/$codeOrLinkFragment"
            attributes["allow"] =
                "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            attributes["allowfullscreen"] = ""
        }
    }
}

@HtmlTagMarker
fun DIV.insertWartelisteInfo() {
    p("d-inline-block") { text("Wenn du dich für ein Shooting anmelden möchtest trage dich einfach direkt in die Warteliste ein:") }
    a("https://service.reisishot.pictures/waitlist", "_blank", "pl-2 btn btn-primary") {
        text("Zur Anmeldung gehen ")
        insertIcon(ReisishotIcons.LINK, "xs", "text-white")
    }
}

@HtmlTagMarker
fun FlowOrPhrasingContent.metadata(metadata: PageMetadata) {
    span("badge bg-light text-secondary text-pre-wrap") {
        metadata.edited?.let {
            time {
                attributes.itemprop = "dateModified"
                dateTime = pictures.reisishot.mise.backend.df_yyyy_MM_dd.format(it)

                span("font-weight-normal") {
                    text("Zuletzt aktualisiert am: ")
                }
                text(it)
            }
            text("  –  ")
        }
        time {
            attributes.itemprop = "datePublished"
            dateTime = pictures.reisishot.mise.backend.df_yyyy_MM_dd.format(metadata.created)
            span("font-weight-normal") {
                text("Veröffentlicht am: ")
            }
            text(metadata.created)
        }
    }
}

@HtmlTagMarker
fun Tag.text(date: Date) {
    text(df_dd_MM_YYYY.format(date))
}

fun <T : Appendable> T.appendUnformattedHtml() = appendHTML(prettyPrint = false, xhtmlCompatible = true)
