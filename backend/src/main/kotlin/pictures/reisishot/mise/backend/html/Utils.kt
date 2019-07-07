package pictures.reisishot.mise.backend.html

import kotlinx.html.*


@HtmlTagMarker
fun HTMLTag.raw(block: StringBuilder.() -> Unit): Unit = consumer.onTagContentUnsafe {
    this.raw(buildString(block))
}

@HtmlTagMarker
fun HTMLTag.raw(content: String): Unit = consumer.onTagContentUnsafe {
    this.raw(content)
}

@HtmlTagMarker
fun FlowContent.div(divId: String, classes: String? = null, block: DIV.() -> Unit = {}): Unit =
    DIV(attributesMapOf("class", classes), consumer).visit {
        id = divId
        block(this)
    }

@HtmlTagMarker
fun FlowContent.container(block: DIV.() -> Unit = {}) = div("container", block)

@HtmlTagMarker
fun FlowContent.fluidContainer(block: DIV.() -> Unit = {}) = div("container-fluid", block)

@HtmlTagMarker
fun FlowContent.photoSwipeHtml() = div(classes = "pswp") {
    attributes["tabindex"] = "-1"
    attributes["role"] = "dialog"
    attributes["aria-hidden"] = "true"
    div(classes = "pswp__bg")
    div(classes = "pswp__scroll-wrap") {
        div(classes = "pswp__container") {
            repeat(3) {
                div(classes = "pswp__item")
            }
        }
        div(classes = "pswp__ui pswp__ui--hidden") {
            div(classes = "pswp__top-bar") {
                div(classes = "pswp__counter")

                button(classes = "pswp__button pswp__button--close") {
                    attributes["shortTitle"] = "Schließen (Esc)"
                }
                button(classes = "pswp__button pswp__button--fs") {
                    attributes["shortTitle"] = "Fullscreen anzeigen"
                }
                button(classes = "pswp__button pswp__button--zoom") {
                    attributes["shortTitle"] = "Zoomen"
                }
                button(classes = "pswp__button pswp__button--details") {
                    attributes["shortTitle"] = "Details"
                }
                div(classes = "pswp__preloader") {
                    div(classes = "pswp__preloader__icn") {
                        div(classes = "pswp__preloader__cut") {
                            div(classes = "pswp__preloader__donut")
                        }
                    }
                }
            }

            div(classes = "pswp__share-modal pswp__share-modal--hidden pswp__single-tap") {
                div(classes = "pswp__share-tooltip")
            }
            button(classes = "pswp__button pswp__button--arrow--left") {
                attributes["shortTitle"] = "Vorheriges Bild"
            }
            button(classes = "pswp__button pswp__button--arrow--right") {
                attributes["shortTitle"] = "Nächstes Bild"
            }
            div(classes = "pswp__caption") {
                div(classes = "pswp__caption__center")
            }
        }
    }
}