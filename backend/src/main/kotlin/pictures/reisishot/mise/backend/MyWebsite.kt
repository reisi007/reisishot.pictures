package pictures.reisishot.mise.backend

import pictures.reisishot.mise.backend.generator.gallery.ExifdataKey
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.ThumbnailGenerator
import pictures.reisishot.mise.backend.generator.gallery.categories.ConfigurableCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.categories.DateCategoryBuilder
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import java.nio.file.Paths

object MyWebsite {

    @JvmStatic
    fun main(args: Array<String>) {
        Mise.build(
            WebsiteConfiguration(
                shortTitle = "Reisishot",
                longTitle = "Reisishot - Fotograf Florian Reisinger",
                inPath = Paths.get("src/main/resources"),
                outPath = Paths.get("../frontend/generated"),
                generators = arrayOf(
                    PageGenerator(),
                    GalleryGenerator(
                        categoryBuilders = *arrayOf(
                            DateCategoryBuilder("Chronologisch"),
                            ConfigurableCategoryBuilder()
                        ), exifReplaceFunction = { cur ->
                            when (cur.first) {
                                ExifdataKey.LENS_MODEL -> when (cur.second) {
                                    "105.0 mm" -> ExifdataKey.LENS_MODEL to "Sigma 105mm EX DG OS HSM"
                                    "147.0 mm" -> ExifdataKey.LENS_MODEL to "Sigma 105mm EX DG OS HSM + 1.4 Sigma EX APO DG Telekonverter"
                                    else -> cur
                                }
                                else -> cur
                            }
                        }
                    ),
                    ThumbnailGenerator(
                        ThumbnailGenerator.ForceRegeneration(
                            thumbnails = false
                        )
                    )
                )
            )
        )
    }
}