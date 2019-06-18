package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.MenuLinkContainer
import pictures.reisishot.mise.backend.generator.SimpleMenuLink
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

object PageGenerator {

    const val LAZYLOADER_CLASSNAME = "lozad"

    fun generatePage(
        target: Path,
        title: String,
        locale: Locale = Locale.getDefault(),
        websiteConfiguration: WebsiteConfiguration,
        additionalHeadContent: HEAD.() -> Unit = {},
        pageContent: DIV.() -> Unit
    ) = with(target) {
        target.parent?.let {
            Files.createDirectories(it)
        }
        Files.newBufferedWriter(target, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE).use {
            it.write("<!doctype html>")
            it.appendHTML(prettyPrint = true, xhtmlCompatible = true)
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
                            nav("navbar navbar-expand-md fixed-top bg-dark") {
                                a(classes = "navbar-brand", href = "/") {
                                    text(websiteConfiguration.title)
                                }
                                button(classes = "navbar-toggler collapsed") {
                                    attributes["type"] = "button"
                                    attributes["data-toggle"] = "collapse"
                                    attributes["data-target"] = "#navbarCollapse"
                                    attributes["aria-controls"] = "navbarCollapse"
                                    attributes["aria-label"] = "Toggle navigation"
                                }
                                div("navbarCollapse", "navbar-collapse collapse") {
                                    ul("navbar-nav mr-auto") {
                                        BuildingCache.menuLinks.forEach { curItem ->
                                            li("nav-item") {
                                                if (curItem is MenuLinkContainer) {
                                                    classes = classes + "dropDown"
                                                    recursivlyBuildMenu(curItem)
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

                        main("flex-shrink-0") {
                            attributes["role"] = "main"
                            fluidContainer {
                                div("content") {
                                    pageContent(this)
                                }
                            }
                        }

                        footer("footer mt-auto py-3") {
                            container {
                                span("text-muted") {
                                    text("© Reisishot (Florian Reisinger)")
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun FlowOrInteractiveOrPhrasingContent.recursivlyBuildMenu(menuLinkContainer: MenuLinkContainer) =
        recursivlyBuildMenu(menuLinkContainer, 1)

    private fun FlowOrInteractiveOrPhrasingContent.recursivlyBuildMenu(
        menuLinkContainer: MenuLinkContainer,
        beginningDropdownId: Int
    ): Int {
        var curDropdownId = beginningDropdownId
        val curIdString = "dropdown$curDropdownId"
        curDropdownId++
        a(classes = "nav-link dropdown-toggle", href = "#") {
            attributes["id"] = curIdString
            attributes["data-toggle"] = "dropdown"
            attributes["aria-haspopup"] = "true"
            text(menuLinkContainer.text)
            val clickable = SimpleMenuLink(-1, menuLinkContainer.href, "Übersicht")
            div("dropdown-menu") {
                attributes["aria-labelledby"] = curIdString
                (sequenceOf(clickable) + menuLinkContainer.children.asSequence()).forEach { menuLink ->
                    when (menuLink) {
                        is SimpleMenuLink -> {
                            a(classes = "dropdown-item", href = menuLink.href) {
                                text(menuLink.text)
                            }
                        }
                        is MenuLinkContainer -> {
                            curDropdownId = recursivlyBuildMenu(menuLink, curDropdownId)
                        }
                    }
                }
            }
        }
        return curDropdownId + 1
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