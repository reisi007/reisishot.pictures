package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.MenuLink
import pictures.reisishot.mise.backend.generator.MenuLinkContainer
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

object PageGenerator {

    const val LAZYLOADER_CLASSNAME = "lazy"
    private const val polyfillUrl = "https://polyfill.io/v3/polyfill.min.js?flags=gated&features=Object.assign%2CIntersectionObserver"

    fun generatePage(
            target: Path,
            title: String,
            locale: Locale = Locale.getDefault(),
            websiteConfiguration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            additionalHeadContent: HEAD.() -> Unit = {},
            pageContent: DIV.() -> Unit
    ): BufferedWriter = with(target) {
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
                            preload()
                            metaUTF8()
                            metaViewport()
                            linkCanonnical(websiteConfiguration, target)
                            favicon()
                            title(title)
                            appCss()
                            additionalHeadContent(this)
                        }
                        body("d-flex flex-column h-100") {
                            header {
                                buildMenu(websiteConfiguration, buildingCache.menuLinks)
                            }

                            main("flex-shrink-0") {
                                attributes["role"] = "main"
                                fluidContainer {
                                    div("container") {
                                        id = "content"
                                        pageContent(this)
                                    }
                                }
                            }

                            footer("footer mt-auto py-3") {
                                container {
                                    a {
                                        attributes["name"] = "footer"
                                    }
                                    websiteConfiguration.createForm(this, target)
                                    p("text-muted center") {
                                        text("© ${websiteConfiguration.longTitle}")
                                    }
                                    p("text-muted center") {
                                        a(buildingCache.getLinkcacheEntryFor(websiteConfiguration, "PAGE", "Impressum")) {
                                            text("Impressum & Datenschutz")
                                        }
                                    }
                                    websiteConfiguration.socialMediaLinks?.let { accounts ->
                                        span("socialIcons") {
                                            accounts.facebook?.let {
                                                a("https://fb.me/$it", "_blank") {
                                                    insertIcon(ReisishotIcons.FB)
                                                }
                                            }
                                            accounts.instagram?.let {
                                                a("https://www.instagram.com/$it/", "_blank") {
                                                    insertIcon(ReisishotIcons.INSTAGRAM)
                                                }
                                            }
                                            accounts.podcast?.let {
                                                a(it, "_blank") {
                                                    insertIcon(ReisishotIcons.PODCAST)
                                                }
                                            }
                                            accounts.whatsapp?.let {
                                                a("https://wa.me/$it", "_blank") {
                                                    insertIcon(ReisishotIcons.WHATSAPP)
                                                }
                                            }
                                            accounts.facebook?.let {
                                                a("https://m.me/$it", "_blank") {
                                                    insertIcon(ReisishotIcons.FB_MESSENGER)
                                                }
                                            }
                                            accounts.mail?.let { mail ->
                                                a("mailto:$mail", "_blank") {
                                                    insertIcon(ReisishotIcons.MAIL)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            polyfills()
                            analyticsJs(websiteConfiguration)
                            cookieInfo()
                            analyticsImage(websiteConfiguration)
                        }
                    }
        }
    }


    private fun HEADER.buildMenu(websiteConfiguration: WebsiteConfiguration, items: Collection<MenuLink>) {
        nav("navbar navbar-dark navbar-fixed-top navbar-expand-md") {
            val navId = "navbarCollapse"
            a(classes = "navbar-brand", href = "/") {
                text(websiteConfiguration.shortTitle)
            }
            if (items.isNotEmpty())
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
                                        a(classes = "dropdown-item") {
                                            accept(entry, websiteConfiguration)
                                        }
                                    }
                                }

                            } else {
                                a(
                                        classes = "nav-link"
                                ) {
                                    accept(curItem, websiteConfiguration)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun A.accept(curItem: MenuLink, websiteConfiguration: WebsiteConfiguration) {
        curItem.target?.let { target = it }
        curItem.href?.let { href = BuildingCache.getLinkFromFragment(websiteConfiguration, it) }
        text(curItem.text)
    }

    @HtmlTagMarker
    private fun BODY.scriptJs(src: String) = script(src = src) {}

    @HtmlTagMarker
    private fun HEAD.appCss() {
        styleLink("/css/styles.css")
    }

    @HtmlTagMarker
    private fun BODY.polyfills() {
        scriptJs(polyfillUrl)
    }

    @HtmlTagMarker
    private fun HEAD.metaUTF8() = meta(charset = "UTF8")

    @HtmlTagMarker
    private fun HEAD.linkCanonnical(websiteConfiguration: WebsiteConfiguration, target: Path) = link(
            rel = "canonical",
            href = websiteConfiguration.websiteLocation + (websiteConfiguration.outPath.relativize(target).parent?.toString()
                    ?: ""))

    @HtmlTagMarker
    private fun HEAD.metaViewport() =
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")

    @HtmlTagMarker
    private fun HEAD.preload() {
        link("/css/rs/fonts/reisishotpictures.woff2", "preload") {
            attributes["as"] = "font"
            attributes["crossorigin"] = ""
        }

        link(polyfillUrl, "preload") {
            attributes["as"] = "script"
        }
        link("/js/combined.min.js", "preload") {
            attributes["as"] = "script"
        }
        link("/css/styles.css", "preload") {
            attributes["as"] = "style"
        }
    }

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
    private fun BODY.cookieInfo() = script("text/javascript", "/js/combined.min.js") {
        attributes.putAll(
                sequenceOf(
                        "id" to "cookieinfo",
                        "data-message" to "Hier werden Cookies verwendet. Wenn Sie fortfahren akzeptieren Sie die Verwendung von Cookies",
                        "data-linkmsg" to "Weitere Informationen zum Datenschutz",
                        "data-moreinfo" to "https://reisishot.pictures/datenschutz",
                        "data-close-text" to "Akzeptieren",
                        "data-accept-on-scroll" to "true"
                )
        )

    }

    @HtmlTagMarker
    private fun BODY.analyticsJs(websiteConfiguration: WebsiteConfiguration) = websiteConfiguration.analyticsSiteId?.let {
        raw("""
        <!-- Matomo -->
        <script type="text/javascript">
          var _paq = window._paq || [];
          _paq.push(['trackPageView']);
          _paq.push(['enableLinkTracking']);
          (function() {
            var u="//analytics.reisishot.pictures/";
            _paq.push(['setTrackerUrl', u+'matomo.php']);
            _paq.push(['setSiteId', '${it}']);
            var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
            g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'matomo.js'; s.parentNode.insertBefore(g,s);
          })();
        </script>
        <!-- End Matomo Code -->
    """.trimIndent().replace("[\n\r]", ""))
    }

    @HtmlTagMarker
    private fun BODY.analyticsImage(websiteConfiguration: WebsiteConfiguration) = websiteConfiguration.analyticsSiteId?.let {
        img(src = """https://analytics.reisishot.pictures/matomo.php?idsite=$it&amp;rec=1""")
    }
}
