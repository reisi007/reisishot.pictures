package pictures.reisishot.mise.backend.generator.gallery.context

import at.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.pictures.reisishot.mise.backend.generator.gallery.AbstractThumbnailGenerator.AbstractThumbnailGeneratorImageSize as ImageSize
import pictures.reisishot.mise.backend.generator.pages.htmlparsing.context.createHtmlApi as baseCreateHtmlApi

@WebsiteConfigBuilderDsl
fun AbstractGalleryGenerator.createHtmlApi() = baseCreateHtmlApi(ImageSize.values, ImageSize.LARGEST, imageFetcher)
