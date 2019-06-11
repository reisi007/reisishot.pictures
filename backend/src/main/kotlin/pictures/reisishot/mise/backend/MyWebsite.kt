package pictures.reisishot.mise.backend

import kotlinx.coroutines.ObsoleteCoroutinesApi
import pictures.reisishot.mise.backend.generator.blog.BlogGenerator
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.ThumbnailGenerator
import pictures.reisishot.mise.backend.generator.gallery.categories.DateCategoryBuilder
import java.nio.file.Paths

object MyWebsite {

    @ObsoleteCoroutinesApi
    @JvmStatic
    fun main(args: Array<String>) {
        Mise.build(
            WebsiteConfiguration(
                "Reisishot - Hobbyfotograf Florian Reisinger",
                Paths.get("../../src/main/resources"),
                generators = arrayOf(
                    BlogGenerator(),
                    GalleryGenerator(
                        DateCategoryBuilder()
                    ),
                    ThumbnailGenerator()
                )
            )
        )
    }
}