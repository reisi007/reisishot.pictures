@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package pictures.reisishot.mise.backend.generator.gallery.context

import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import at.reisishot.mise.commons.ComplexName
import at.reisishot.mise.exifdata.ExifdataKey
import kotlinx.html.div
import kotlinx.html.p
import pictures.reisishot.mise.backend.config.category.CategoryInformation
import pictures.reisishot.mise.backend.config.category.flatten
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.insertCategoryThumbnails
import pictures.reisishot.mise.backend.generator.gallery.insertSubcategoryThumbnails
import pictures.reisishot.mise.backend.html.appendUnformattedHtml
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@WebsiteConfigBuilderDsl
fun AbstractGalleryGenerator.createCategoryApi(): Pair<String, VelocityTemplateObjectCreator> =
    "categories" to { _, websiteConfig, _ ->
        CategoryApi(this, websiteConfig)
    }

internal class CategoryApi(
    private val galleryGenerator: AbstractGalleryGenerator,
    private val websiteConfig: WebsiteConfig,
) : TemplateObject {
    private val cache: AbstractGalleryGenerator.Cache
        get() = galleryGenerator.cache

    private val galleryInfoDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")

    fun insertSubalbumThumbnails(albumName: String): String = buildString {
        appendUnformattedHtml().div {
            val categoryInformation = cache.rootCategory
            val subcategories: Set<CategoryInformation> = if (albumName.isBlank()) {
                categoryInformation
            } else {
                categoryInformation.flatten()
                    .find { it.categoryName.complexName.equals(albumName, true) }
                    ?.subcategories
                    ?: error("No category $albumName found")
            }

            insertSubcategoryThumbnails(galleryGenerator, subcategories, websiteConfig)
        }
    }

    fun insertCategoryOverview(vararg albumName: String) = buildString {
        if (albumName.isEmpty()) return@buildString
        @Suppress("RemoveExplicitTypeArguments")
        val albumNameSet = setOf<ComplexName>(*albumName)

        appendUnformattedHtml().div {
            val albums = cache
                .rootCategory
                .flatten()
                .filter {
                    val name = it.categoryName.complexName
                    albumNameSet.contains(name)
                }
                .toSet()

            if (albumName.size != albums.size) {
                error("Not all categories found! $albumName")
            }
            insertCategoryThumbnails(galleryGenerator, albums, websiteConfig)
        }
    }

    fun insertGalleryInfo(): String = buildString {
        appendUnformattedHtml().p {
            val images = cache.imageInformationData.values
            val count = images.size
            val prep = images.asSequence()
                .map { it as? InternalImageInformation }
                .filterNotNull()
                .map { it.exifInformation.get(ExifdataKey.CREATION_DATETIME) }
                .filterNotNull()
                .map { ZonedDateTime.parse(it) }

            val lastImage = prep.maxOrNull()
            val firstImage = prep.minOrNull()

            text("Auf dieser Webseite sind insgesamt ")
            text(count)
            text(" Bilder")
            if (firstImage != null && lastImage != null) {
                text(" aufgenommen zwischen ")
                text(galleryInfoDateTimeFormatter.format(firstImage))
                text(" und ")
                text(galleryInfoDateTimeFormatter.format(lastImage))
            }
            text(" zu sehen.")
        }
    }
}
