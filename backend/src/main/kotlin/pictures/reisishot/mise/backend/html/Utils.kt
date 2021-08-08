package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import kotlinx.html.impl.DelegatingMap
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.df_dd_MM_YYYY
import pictures.reisishot.mise.backend.df_yyyy_MM_dd
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.CategoryInformation
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.getOrThrow
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.pages.Testimonial
import pictures.reisishot.mise.backend.generator.pages.minimalistic.TargetPath
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata
import java.util.*


@HtmlTagMarker
fun HTMLTag.raw(block: StringBuilder.() -> Unit): Unit = consumer.onTagContentUnsafe {
    this.raw(buildString(block))
}

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
fun FlowContent.fluidContainer(block: DIV.() -> Unit = {}) = div("container-fluid", block)

@HtmlTagMarker
fun HtmlBlockTag.insertImageGallery(
    galleryName: String,
    configuration: WebsiteConfiguration,
    imageInformation: List<ImageInformation>
) = with(imageInformation) {
    if (isEmpty())
        return@with
    div {
        val isSingleImageGallery = imageInformation.size == 1
        classes = classes + "gallery center mt-3" + if (isSingleImageGallery) "single" else "row multiple"
        attributes["data-name"] = galleryName
        val additionalClasses = if (isSingleImageGallery)
            ""
        else
            "col-12 col-sm-6 col-lg-4 col-xl-3 col-xxl-2"
        imageInformation.forEach { curImageInfo ->
            insertLazyPicture(curImageInfo, configuration, additionalClasses)
        }
    }
}

fun HtmlBlockTag.insertImageGallery(
    galleryName: String,
    configuration: WebsiteConfiguration,
    vararg imageInformation: ImageInformation
) = insertImageGallery(galleryName, configuration, listOf(*imageInformation))

internal fun HtmlBlockTag.insertLazyPicture(
    curImageInfo: ImageInformation,
    configuration: WebsiteConfiguration,
    additionalClasses: String? = null
) {
    div("pic-holder " + (additionalClasses ?: "")) {
        div(PageGenerator.LAZYLOADER_CLASSNAME) {
            attributes["data-alt"] = curImageInfo.title
            attributes["data-id"] = curImageInfo.filename
            attributes["data-url"] = curImageInfo.getUrl(configuration)
            curImageInfo.thumbnailSizes.values.first().let {
                style = "padding-top: ${(it.height * 100f) / it.width}%"
            }
            ImageSize.values().forEachIndexed { idx, curSize ->
                val thumbnailSize = curImageInfo.thumbnailSizes.getValue(curSize)
                attributes["data-$idx"] = """{
                    |"jpg":"${curImageInfo.getJpgUrl(configuration, curSize)}",
                    |"webp":"${curImageInfo.getWebPUrl(configuration, curSize)}",
                    |"w":${thumbnailSize.width},
                    |"h":${thumbnailSize.height}
                    |}""".trimMargin()
            }
            attributes["data-sizes"] = ImageSize.values().size.toString()
            noScript {
                img(curImageInfo.title, curImageInfo.getJpgUrl(configuration, ImageSize.LARGEST))
            }
        }
    }
}

private fun ImageInformation.getJpgUrl(configuration: WebsiteConfiguration, imageSize: ImageSize): String {
    return getUrl(configuration).substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".jpg"
}

private fun ImageInformation.getWebPUrl(configuration: WebsiteConfiguration, imageSize: ImageSize): String {
    return getUrl(configuration).substringBefore("gallery/images") + "images/" + filename + '_' + imageSize.identifier + ".webp"
}

internal fun HtmlBlockTag.insertSubcategoryThumbnail(
    categoryInformation: CategoryInformation,
    imageInformation: ImageInformation,
    configuration: WebsiteConfiguration
) {
    a("/gallery/categories/${categoryInformation.urlFragment}", classes = "card") {
        div("card-img-top") {
            insertLazyPicture(imageInformation, configuration)
        }
        div("card-body") {
            h4("card-title") {
                text(categoryInformation.displayName)
            }
        }
    }
}

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

internal var DelegatingMap.itemprop
    get() = this["itemprop"]
    set(value) {
        if (value == null)
            this.remove("itemprop")
        else
            this["itemprop"] = value
    }

internal var DelegatingMap.itemscope
    get() = this["itemscope"]
    set(value) {
        if (value == null)
            this.remove("itemscope")
        else
            this["itemscope"] = value
    }

internal var DelegatingMap.itemtype
    get() = this["itemtype"]
    set(value) {
        if (value == null)
            this.remove("itemtype")
        else
            this["itemtype"] = value
    }

object Itemtypes {
    const val ARTICLE = "http://schema.org/Article"
}

internal var DelegatingMap.content
    get() = this["content"]
    set(value) {
        if (value == null)
            this.remove("content")
        else
            this["content"] = value
    }

@HtmlTagMarker
fun DIV.renderTestimonial(
    websiteConfiguration: WebsiteConfiguration,
    targetPath: TargetPath,
    galleryGenerator: AbstractGalleryGenerator,
    testimonial: Testimonial
) {
    div("col-12 col-lg-5 card border-black") {
        attributes.itemprop = "review"
        attributes.itemprop = ""
        attributes.itemtype = "https://schema.org/Review"
        with(galleryGenerator.cache) {
            if (testimonial.video != null) {
                insertYoutube(testimonial.video, 4, 5, "card-img-top")
            } else if (testimonial.image != null) {
                val curImageInfo = imageInformationData.getOrThrow(testimonial.image, targetPath)
                insertLazyPicture(curImageInfo, websiteConfiguration, "card-img-top")
            } else throw IllegalStateException("No media available for testimonial $testimonial")
        }
        div("card-body") {
            h5("card-title") {
                span {
                    attributes.itemprop = "author"
                    attributes.itemtype = "https://schema.org/Person"
                    text(testimonial.name)
                }
                br()
                small("text-muted") {
                    attributes.itemprop = "datePublished"
                    attributes.content = testimonial.isoDateString
                    text(testimonial.formattedDate)
                }
            }
            div("card-text") {
                attributes.itemprop = "reviewBody"
                raw(testimonial.html)
            }
        }
    }
}

@HtmlTagMarker
fun StringBuilder.appendTestimonials(
    websiteConfiguration: WebsiteConfiguration,
    targetPath: TargetPath,
    galleryGenerator: AbstractGalleryGenerator,
    vararg testimonialsToDisplay: Testimonial
) {
    appendHTML(false, true).div {
        div("container-flex reviews") {
            testimonialsToDisplay.forEach { testimonial ->
                renderTestimonial(websiteConfiguration, targetPath, galleryGenerator, testimonial)
            }

        }
    }
}

@HtmlTagMarker
internal fun FlowOrPhrasingContent.metadata(metadata: PageMetadata) {
    span("badge bg-light text-secondary text-pre-wrap") {
        metadata.edited?.let {
            time {
                attributes.itemprop = "dateModified"
                dateTime = df_yyyy_MM_dd.format(it)

                span("font-weight-normal") {
                    text("Zuletzt aktualisiert am: ")
                }
                text(it)
            }
            text("  –  ")
        }
        time {
            attributes.itemprop = "datePublished"
            dateTime = df_yyyy_MM_dd.format(metadata.created)
            span("font-weight-normal") {
                text("Veröffentlicht am: ")
            }
            text(metadata.created)
        }
    }
}

@HtmlTagMarker
internal fun Tag.text(date: Date) {
    text(df_dd_MM_YYYY.format(date))
}

@HtmlTagMarker
internal fun HtmlBlockTag.insertYoutube(codeOrLinkFragment: String, w: Int, h: Int, vararg additionalClasses: String) {
    p("ratio") {
        classes = classes + ("ratio-${w}x$h")
        if (!additionalClasses.isNullOrEmpty())
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


fun <T : Appendable> T.appendUnformattedHtml() = appendHTML(false, true)
