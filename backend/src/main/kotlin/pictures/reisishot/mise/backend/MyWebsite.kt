package pictures.reisishot.mise.backend

import kotlinx.coroutines.ObsoleteCoroutinesApi
import pictures.reisishot.mise.backend.generator.blog.BlogGenerator
import pictures.reisishot.mise.backend.generator.gallery.ExifdataKey
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.ThumbnailGenerator
import pictures.reisishot.mise.backend.generator.gallery.categories.ConfigurableCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.categories.DateCategoryBuilder
import java.nio.file.Paths

object MyWebsite {

    @ObsoleteCoroutinesApi
    @JvmStatic
    fun main(args: Array<String>) {
        Mise.build(
            WebsiteConfiguration(
                title = "Reisishot - Hobbyfotograf Florian Reisinger",
                inFolder = Paths.get("src/main/resources"),
                outFolder = Paths.get("../frontend/generated"),
                generators = arrayOf(
                    BlogGenerator(),
                    GalleryGenerator(
                        categoryBuilders = *arrayOf(
                            DateCategoryBuilder(),
                            ConfigurableCategoryBuilder()
                        ), exifReplaceFunction = { cur ->
                            when (cur.first) {
                                ExifdataKey.LENS_MODEL -> when (cur.second) {
                                    "105 mm" -> ExifdataKey.LENS_MODEL to "Sigma 105mm EX DG OS HSM"
                                    "147 mm" -> ExifdataKey.LENS_MODEL to "Sigma 105mm EX DG OS HSM + 1.4 Sigma EX APO DG Teleconverter"
                                    else -> cur
                                }
                                else -> cur
                            }
                        }
                    ),
                    ThumbnailGenerator()
                )
            )
        )
    }
}