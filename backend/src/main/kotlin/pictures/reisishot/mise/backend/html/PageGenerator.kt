package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.MenuLink
import pictures.reisishot.mise.backend.generator.MenuLinkContainer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

object PageGenerator {

    const val LAZYLOADER_CLASSNAME = "lazy"

    fun generatePage(
            target: Path,
            title: String,
            locale: Locale = Locale.getDefault(),
            websiteConfiguration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            hasGallery: Boolean = true,
            additionalHeadContent: HEAD.() -> Unit = {},
            pageContent: DIV.() -> Unit
    ) = with(target) {
        target.parent?.let {
            Files.createDirectories(it)
        }
        Files.newBufferedWriter(target, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE).use {
            it.write("<!doctype html>")
            it.appendHTML(prettyPrint = false, xhtmlCompatible = true)
                    .html(namespace = "http://www.w3.org/1999/xhtml") {
                        classes = classes + "h-100"
                        head {
                            lang = locale.toLanguageTag()

                            metaUTF8()
                            metaViewport()
                            favicon()

                            title(title)

                            vendorCss()
                            appCss()

                            polyfills()
                            googleAnalytics()
                            vendorJs()
                            appJs()

                            additionalHeadContent(this)
                        }
                        body("d-flex flex-column h-100") {
                            header {
                                buildMenu(websiteConfiguration, buildingCache.menuLinks)
                            }

                            main("flex-shrink-0") {
                                attributes["role"] = "main"
                                fluidContainer {
                                    divId("content") {
                                        pageContent(this)
                                    }
                                }
                            }

                            footer("footer mt-auto py-3") {
                                container {
                                    a {
                                        attributes["name"] = "footer"
                                    }
                                    span("text-muted") {
                                        text("© ${websiteConfiguration.longTitle}")
                                    }
                                    span("socialIcons") {
                                        a("https://fb.me/reisishot", "_blank") {
                                            insertIcon(ReisishotIcons.FB)
                                        }
                                        a("https://www.instagram.com/reisishot/", "_blank") {
                                            insertIcon(ReisishotIcons.INSTAGRAM)
                                        }
                                        a("https://m.me/reisishot", "_blank") {
                                            insertIcon(ReisishotIcons.FB_MESSENGER)
                                        }
                                        a("mailto:florian@reisishot.pictures", "_blank") {
                                            insertIcon(ReisishotIcons.MAIL)
                                        }
                                    }
                                }
                            }
                            cookieInfo()
                        }
                    }
        }
    }


    private fun HEADER.buildMenu(websiteConfiguration: WebsiteConfiguration, items: Collection<MenuLink>) {
        nav("navbar navbar-dark fixed-top navbar-expand-lg") {
            val navId = "navbarCollapse"
            a(classes = "navbar-brand", href = "/") {
                text(websiteConfiguration.shortTitle)
            }
            button(classes = "navbar-toggler") {
                attributes["type"] = "button"
                attributes["data-toggle"] = "collapse"
                attributes["data-target"] = "#$navId"
                attributes["aria-controls"] = navId
                attributes["aria-expanded"] = "false"
                attributes["aria-label"] = "Toggle navigation"
                span("navbar-toggler-icon")
            }

            var dropdownCount = 0;
            divId("navbarCollapse", "navbar-collapse collapse") {
                ul("navbar-nav navbar-light mr-auto") {
                    items.forEach { curItem ->
                        li("nav-item") {
                            if (curItem is MenuLinkContainer) {
                                classes = classes + " dropdown"
                                val dropDownId = "dropDown$dropdownCount"
                                dropdownCount++
                                a(classes = "nav-link dropdown-toggle", href = "#") {
                                    attributes["id"] = dropDownId
                                    attributes["role"] = "button"
                                    attributes["data-toggle"] = "dropdown"
                                    attributes["aria-haspopup"] = "true"
                                    attributes["aria-expanded"] = "false"
                                    text(curItem.text)
                                }
                                div(classes = "dropdown-menu") {
                                    attributes["aria-labelledby"] = dropDownId
                                    curItem.children.forEach { entry ->
                                        a(
                                                classes = "dropdown-item",
                                                href = websiteConfiguration.websiteLocation + entry.href
                                        ) {
                                            text(entry.text)
                                        }
                                    }
                                }

                            } else {
                                a(
                                        classes = "nav-link",
                                        href = websiteConfiguration.websiteLocation + curItem.href
                                ) {
                                    text(curItem.text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @HtmlTagMarker
    private fun HEAD.script(src: String) = script(src = src) {}

    @HtmlTagMarker
    private fun HEAD.appCss() = styleLink("/css/app.css")

    @HtmlTagMarker
    private fun HEAD.vendorCss() = styleLink("/css/vendor.css")

    @HtmlTagMarker
    private fun HEAD.appJs() = script("/js/app.min.js")

    @HtmlTagMarker
    private fun HEAD.vendorJs() = script("/js/vendor.js")

    @HtmlTagMarker
    private fun HEAD.polyfills() {
        script("https://polyfill.io/v3/polyfill.min.js?features=IntersectionObserver")
    }

    @HtmlTagMarker
    private fun HEAD.googleAnalytics() = script(src = "https://www.googletagmanager.com/gtag/js?id=UA-120917271-1") {
        attributes["async"] = ""
    }

    @HtmlTagMarker
    private fun HEAD.metaUTF8() = meta(charset = "UTF8")

    @HtmlTagMarker
    private fun HEAD.metaViewport() =
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")

    @HtmlTagMarker
    private fun HEAD.favicon() {
        link("/apple-icon-57x57.png", "apple-touch-icon") { attributes["sizes"] = "57x57" }
        link("/apple-icon-60x60.png", "apple-touch-icon") { attributes["sizes"] = "60x60" }
        link("/apple-icon-72x72.png", "apple-touch-icon") { attributes["sizes"] = "72x72" }
        link("/apple-icon-76x76.png", "apple-touch-icon") { attributes["sizes"] = "76x76" }
        link("/apple-icon-114x114.png", "apple-touch-icon") { attributes["sizes"] = "114x114" }
        link("/apple-icon-120x120.png", "apple-touch-icon") { attributes["sizes"] = "120x120" }
        link("/apple-icon-144x144.png", "apple-touch-icon") { attributes["sizes"] = "144x144" }
        link("/apple-icon-152x152.png", "apple-touch-icon") { attributes["sizes"] = "152x152" }
        link("/apple-icon-180x180.png", "apple-touch-icon") { attributes["sizes"] = "180x180" }
        link("/android-icon-192x192.png", "icon") {
            attributes["sizes"] = "192x192"
            attributes["type"] = "image/png"
        }
        link("/favicon-32x32.png", "icon") {
            attributes["sizes"] = "32x32"
            attributes["type"] = "image/png"
        }
        link("/favicon-96x96.png", "icon") {
            attributes["sizes"] = "96x96"
            attributes["type"] = "image/png"
        }
        link("/favicon-16x16.png", "icon") {
            attributes["sizes"] = "16x16"
            attributes["type"] = "image/png"
        }

        meta("msapplication-TileColor", "#ffffff")
        meta("msapplication-TileColor", "#ffffff")
        meta("msapplication-TileColor", "#ffffff")
    }

    @HtmlTagMarker
    private fun BODY.cookieInfo() = script("text/javascript", "//cookieinfoscript.com/js/cookieinfo.min.js") {
        attributes.putAll(
                sequenceOf(
                        "id" to "cookieinfo",
                        "data-message" to "Hier werden Cookies verwendet. Wenn Sie fortfahren akzeptieren Sie die Verwendung von Cookies",
                        "data-linkmsg" to "Weitere Informationen zu Cookies",
                        "data-moreinfo" to "https://de.wikipedia.org/wiki/HTTP-Cookie",
                        "data-close-text" to "Akzeptieren",
                        "data-accept-on-scroll" to "true"
                )
        )

    }
}
