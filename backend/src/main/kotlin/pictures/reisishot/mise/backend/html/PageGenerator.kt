package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.generator.gallery.FilenameWithoutExtension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

object PageGenerator {

    private const val VERSION_BOOTSTRAP = "4.3.1"
    private const val VERSION_JQUERY = "3.3.1"
    private const val VERSION_POPPER = "1.14.7"

    fun generatePage(
        target: Path,
        title: String,
        locale: Locale = Locale.getDefault(),
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
                    body {
                        //TODO Add menus and so on

                        div("content") {
                            pageContent(this)
                        }

                        //TODO add footer
                    }
                }
        }
    }

    fun HtmlBlockTag.singleImageGallery(imageFolder: Path, imageName: FilenameWithoutExtension) {
        h1 {
            text("Here should be an awesome image! ($imageName)")
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