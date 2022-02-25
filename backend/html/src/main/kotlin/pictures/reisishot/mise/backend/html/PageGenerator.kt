package pictures.reisishot.mise.backend.html

import kotlinx.html.A
import kotlinx.html.BODY
import kotlinx.html.DIV
import kotlinx.html.HEAD
import kotlinx.html.HEADER
import kotlinx.html.HtmlTagMarker
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.footer
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.lang
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.styleLink
import kotlinx.html.title
import kotlinx.html.ul
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.MenuLink
import pictures.reisishot.mise.backend.config.MenuLinkContainer
import pictures.reisishot.mise.backend.config.MenuLinkContainerItem
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.html.config.FacebookMessengerChatPlugin
import pictures.reisishot.mise.backend.html.config.htmlConfig
import java.io.BufferedWriter
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale

object PageGenerator {

    const val LAZYLOADER_CLASSNAME = "lazy"
    private const val polyfillUrl =
        "https://polyfill.io/v3/polyfill.min.js?flags=gated&features=Object.assign%2CIntersectionObserver"

    fun generatePage(
        target: Path,
        title: String,
        locale: Locale = Locale.getDefault(),
        websiteConfig: WebsiteConfig,
        buildingCache: BuildingCache,
        additionalHeadContent: HEAD.() -> Unit = {},
        isMinimalPage: Boolean = false,
        pageContent: DIV.() -> Unit
    ): BufferedWriter = with(target) {
        val htmlConfig = websiteConfig.htmlConfig
        target.parent?.let {
            Files.createDirectories(it)
        }
        Files.newBufferedWriter(target, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
            .use { writer ->
                writer.write("<!doctype html>")
                writer.appendUnformattedHtml()
                    .html(namespace = "http://www.w3.org/1999/xhtml") {
                        val url = BuildingCache.getLinkFromFragment(
                            websiteConfig,
                            websiteConfig.paths.targetFolder.relativize(target)
                                .parent
                                ?.toString()
                                ?: ""
                        )
                        classes = classes + "h-100"
                        head {
                            lang = locale.toLanguageTag()
                            preload(websiteConfig, isMinimalPage)
                            metaUTF8()
                            metaViewport()
                            linkCanonnical(url)
                            favicon()
                            title(title)
                            appCss(websiteConfig)
                            pwaBuilder()
                            additionalHeadContent(this)
                        }
                        body("d-flex flex-column h-100") {
                            if (!isMinimalPage)
                                header {
                                    buildMenu(websiteConfig, buildingCache.menuLinks)
                                }

                            main("flex-shrink-0") {
                                attributes["role"] = "main"
                                div("container-xxxl") {
                                    id = "content"
                                    socialSharing(url, title)
                                    pageContent(this)
                                }
                            }

                            footer("footer mt-auto py-3") {
                                container {
                                    a {
                                        attributes["name"] = "footer"
                                    }
                                    htmlConfig.formBuilder?.let { it(this, target, websiteConfig) }

                                    p("text-muted center") {
                                        text("© ${websiteConfig.websiteInformation.longTitle}")
                                    }
                                    p("text-muted center") {
                                        a(buildingCache.getLinkcacheEntryFor(websiteConfig, "PAGE", "impressum")) {
                                            text("Impressum & Datenschutz")
                                        }
                                    }
                                    htmlConfig.socialMediaLinks?.let { accounts ->
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
                            analyticsJs(websiteConfig)
                            cookieInfo(websiteConfig.miseConfig.isDevMode)
                            analyticsImage(websiteConfig)
                            if (!isMinimalPage)
                                htmlConfig.fbMessengerChatPlugin?.let { fbChat(it) }
                        }
                    }
            }
    }

    private fun HEADER.buildMenu(
        websiteConfig: WebsiteConfig,
        items: Collection<MenuLink>
    ) {
        nav("navbar navbar-dark navbar-fixed-top navbar-expand-${websiteConfig.htmlConfig.bootsrapMenuBreakpoint}") {
            val navId = "navbarCollapse"
            div("container-fluid") {
                a(classes = "navbar-brand", href = "/") {
                    websiteConfig.htmlConfig.navbarBrandCreator
                        ?.let { it(this, websiteConfig) }
                        ?: run { text(websiteConfig.websiteInformation.shortTitle) }
                }
                if (items.isNotEmpty())
                    button(classes = "navbar-toggler") {
                        attributes["type"] = "button"
                        attributes["data-bs-toggle"] = "collapse"
                        attributes["data-bs-target"] = "#$navId"
                        attributes["aria-controls"] = navId
                        attributes["aria-expanded"] = "false"
                        attributes["aria-label"] = "Toggle navigation"
                        span("navbar-toggler-icon")
                    }

                var dropdownCount = 0
                divId("navbarCollapse", "navbar-collapse collapse") {
                    ul("navbar-nav navbar-light mr-auto") {
                        items.forEach { curItem ->
                            li("nav-item") {
                                if (curItem is MenuLinkContainer) {
                                    classes = classes + " dropdown"
                                    val dropDownId = "dropDown$dropdownCount"
                                    dropdownCount++
                                    a(classes = "nav-link dropdown-toggle", href = "/") {
                                        attributes["id"] = dropDownId
                                        attributes["role"] = "button"
                                        attributes["data-bs-toggle"] = "dropdown"
                                        attributes["aria-haspopup"] = "true"
                                        attributes["aria-expanded"] = "false"
                                        text(curItem.text)
                                    }
                                    div(classes = "dropdown-menu") {
                                        attributes["aria-labelledby"] = dropDownId
                                        curItem.children.forEach { entry ->
                                            a(classes = "dropdown-item") {
                                                accept(entry, websiteConfig)
                                            }
                                        }
                                    }
                                } else if (curItem is MenuLinkContainerItem) {
                                    a(
                                        classes = "nav-link"
                                    ) {
                                        accept(curItem, websiteConfig)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun A.accept(curItem: MenuLinkContainerItem, websiteConfig: WebsiteConfig) {
        curItem.target?.let { target = it }
        curItem.href.let { href = BuildingCache.getLinkFromFragment(websiteConfig, it) }
        text(curItem.text)
    }

    @HtmlTagMarker
    private fun BODY.scriptJs(src: String) = script(src = src) {}

    @HtmlTagMarker
    private fun HEAD.appCss(configuration: WebsiteConfig) {
        val prefix = getRessourceUrlPrefix(configuration.miseConfig.isDevMode)
        styleLink("$prefix/css/styles.css")
    }

    @HtmlTagMarker
    private fun BODY.polyfills() {
        scriptJs(polyfillUrl)
    }

    @HtmlTagMarker
    private fun HEAD.metaUTF8() = meta(charset = "UTF8")

    @HtmlTagMarker
    private fun HEAD.linkCanonnical(url: String) = link(
        rel = "canonical",
        href = url
    )

    @HtmlTagMarker
    private fun HEAD.metaViewport() =
        meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")

    @HtmlTagMarker
    private fun HEAD.preload(configuration: WebsiteConfig, isMinimal: Boolean) {
        val prefix = getRessourceUrlPrefix(configuration.miseConfig.isDevMode)
        link("$prefix/css/rs/fonts/reisishotpictures.woff2", "preload") {
            attributes["as"] = "font"
            attributes["crossorigin"] = ""
        }
        link(polyfillUrl, "preload") {
            attributes["as"] = "script"
        }
        if (!(isMinimal || configuration.htmlConfig.fbMessengerChatPlugin == null))
            link("https://connect.facebook.net/de_DE/sdk/xfbml.customerchat.js", "preload") {
                attributes["as"] = "script"
            }
        link("$prefix/js/combined.min.js", "preload") {
            attributes["as"] = "script"
        }
        link("$prefix/css/styles.css", "preload") {
            attributes["as"] = "style"
        }
    }

    private fun getRessourceUrlPrefix(devMode: Boolean) = if (devMode) "" else "https://static.reisishot.pictures"

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


        link("/manifest.json") {
            rel = "manifest"
        }
    }

    @HtmlTagMarker
    private fun DIV.socialSharing(url: String, title: String) = div("socialShare") {
        div("content") {
            text("Diese Seite teilen:")
            a("https://www.facebook.com/sharer.php?u=$url", "_blank") {
                insertIcon(ReisishotIcons.FB)
            }
            a("https://api.whatsapp.com/send?&text=${"$title $url".encoded()}", "_blank") {
                insertIcon(ReisishotIcons.WHATSAPP)
            }
            a("mailto:?subject=$title&body=$url", "_blank") {
                insertIcon(ReisishotIcons.MAIL)
            }
        }
    }

    private fun String.encoded() = URLEncoder.encode(this, Charsets.UTF_8.displayName())

    @HtmlTagMarker
    private fun BODY.cookieInfo(devMode: Boolean) {
        val prefix = getRessourceUrlPrefix(devMode)
        val attributes = sequenceOf(
            "id" to "cookieinfo",
            "data-message" to "Hier werden Cookies verwendet. Wenn Sie fortfahren akzeptieren Sie die Verwendung von Cookies",
            "data-linkmsg" to "Weitere Informationen zum Datenschutz",
            "data-moreinfo" to "https://reisishot.pictures/datenschutz",
            "data-close-text" to "Akzeptieren",
            "data-accept-on-scroll" to "true"
        )
        script("text/javascript", "$prefix/js/combined.min.js") {
            this.attributes.putAll(attributes)
        }
    }

    @HtmlTagMarker
    private fun BODY.analyticsJs(websiteConfig: WebsiteConfig) =
        websiteConfig.htmlConfig.matomoId?.let {
            raw(
                """
        <!-- Matomo -->
        <script type="text/javascript">
          var _paq = window._paq || [];
          _paq.push(['trackPageView']);
          _paq.push(['enableLinkTracking']);
          (function() {
            var u="//analytics.reisishot.pictures/";
            _paq.push(['setTrackerUrl', u+'matomo.php']);
            _paq.push(['setSiteId', '$it']);
            var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
            g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'matomo.js'; s.parentNode.insertBefore(g,s);
          })();
        </script>
        <!-- End Matomo Code -->
                """.trimIndent()
                    .replace("[\n\r]", "")
            )
        }

    @HtmlTagMarker
    private fun BODY.analyticsImage(websiteConfig: WebsiteConfig) =
        websiteConfig.htmlConfig.matomoId?.let {
            img(src = """https://analytics.reisishot.pictures/matomo.php?idsite=$it&amp;rec=1""") {
                height = "0"
            }
        }

    @HtmlTagMarker
    private fun BODY.fbChat(data: FacebookMessengerChatPlugin) {
        divId("fb-root")
        div("fb-customerchat") {
            attributes["page_id"] = data.pageId.toString()
            attributes["theme_color"] = data.themeColor
            attributes["logged_in_greeting"] = data.message
            attributes["logged_out_greeting"] = data.message
            attributes["greeting_dialog_display"] = "hide"
        }
    }
}

private fun HEAD.pwaBuilder() {
    script("module") {
        //language=JavaScript
        +"""
   import 'https://cdn.jsdelivr.net/npm/@pwabuilder/pwaupdate';
   const el = document.createElement('pwa-update');
   document.body.appendChild(el);
        """.trimIndent()
    }
}
