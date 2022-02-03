@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package pictures.reisishot.mise.backend.generator.gallery.context

import kotlinx.html.div
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import pictures.reisishot.mise.backend.config.category.CategoryInformation
import pictures.reisishot.mise.backend.config.category.flatten
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.insertCategoryThumbnails
import pictures.reisishot.mise.backend.generator.gallery.insertSubcategoryThumbnails
import pictures.reisishot.mise.backend.html.appendUnformattedHtml
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import pictures.reisishot.mise.commons.ComplexName

@WebsiteConfigBuilderDsl
fun AbstractGalleryGenerator.createCategoryApi(): Pair<String, VelocityTemplateObjectCreator> =
    "categories" to { _, websiteConfig, _ ->
        CategoryApi(this.cache, websiteConfig)
    }

internal class CategoryApi(
    private val cache: AbstractGalleryGenerator.Cache,
    private val websiteConfig: WebsiteConfig,
) : TemplateObject {
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

            insertSubcategoryThumbnails(subcategories, websiteConfig)
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
            insertCategoryThumbnails(albums, websiteConfig)
        }
    }
}
