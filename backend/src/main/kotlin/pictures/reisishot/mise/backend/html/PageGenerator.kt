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

                        title(title)

                        vendorCss()
                        appCss()

                        polyfills()
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
                                span("text-muted") {
                                    text("© ${websiteConfiguration.longTitle}")
                                }
                            }
                        }
                        if (hasGallery)
                            photoSwipeHtml()
                    }
                }
        }
    }

    private fun HEADER.buildMenu(websiteConfiguration: WebsiteConfiguration, items: Set<MenuLink>) {
        nav("navbar navbar-dark navbar-expand-md") {
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
                                a(classes = "nav-link dropdown-toggle", href = curItem.href ?: "#") {
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
                                        a(classes = "dropdown-item", href = entry.href) {
                                            text(entry.text)
                                        }
                                    }
                                }

                            } else {
                                a(classes = "nav-link", href = curItem.href) {
                                    text(curItem.text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun HEAD.script(src: String) = script(src = src) {}

    private fun HEAD.appCss() = styleLink("/css/app.css")
    private fun HEAD.vendorCss() = styleLink("/css/vendor.css")

    private fun HEAD.appJs() = script("/js/app.js")
    private fun HEAD.vendorJs() = script("/js/vendor.js")
    private fun HEAD.polyfills() {
        script("https://polyfill.io/v3/polyfill.min.js?features=IntersectionObserver")
    }

    private fun HEAD.metaUTF8() = meta(charset = "UTF8")

    private fun HEAD.metaViewport() =
        meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")
}
