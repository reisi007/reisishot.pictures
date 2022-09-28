@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package pictures.reisishot.mise.backend.generator.gallery.context

import kotlinx.html.div
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import pictures.reisishot.mise.backend.config.category.CategoryInformation
import pictures.reisishot.mise.backend.config.category.flatten
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.insertCategoryThumbnails
import pictures.reisishot.mise.backend.generator.gallery.insertImageGallery
import pictures.reisishot.mise.backend.generator.gallery.insertSubcategoryThumbnails
import pictures.reisishot.mise.backend.html.appendUnformattedHtml
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import pictures.reisishot.mise.commons.ComplexName

@WebsiteConfigBuilderDsl
fun AbstractGalleryGenerator.createCategoryApi(): Pair<String, VelocityTemplateObjectCreator> =
    "categories" to { _, websiteConfig, _ ->
        CategoryApi(this, websiteConfig)
    }

internal class CategoryApi(
    private val galleryGenerator: AbstractGalleryGenerator,
    private val configuration: WebsiteConfig,
) : TemplateObject {
    private val cache = galleryGenerator.cache

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

            insertSubcategoryThumbnails(subcategories, configuration)
        }
    }

    fun insertCategoryOverview(vararg categoryNames: String) = buildString {
        @Suppress("RemoveExplicitTypeArguments")
        val albumNameSet = categoryNames.toSet<ComplexName>()

        appendUnformattedHtml().div {
            val categories = findAlbumsWithName(albumNameSet)
                .toSet()

            if (categoryNames.size != categories.size) {
                error("Not all categories found! $categoryNames")
            }
            insertCategoryThumbnails(categories, configuration)
        }
    }

    fun insertImagesForCategory(albumName: String) = buildString {
        val category = findAlbumsWithName(setOf(albumName))
            .firstOrNull() ?: throw IllegalStateException("An album with name \"$albumName\" has not been found!")
        appendUnformattedHtml().div {
            insertImageGallery(galleryGenerator, configuration, category.images)
        }
    }

    private fun findAlbumsWithName(albumNameSet: Set<ComplexName>) =
        cache.rootCategory
            .flatten()
            .filter {
                val name = it.categoryName.complexName
                albumNameSet.contains(name)
            }
}
