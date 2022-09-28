@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package pictures.reisishot.mise.backend.generator.gallery.context

import kotlinx.html.a
import kotlinx.html.div
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import pictures.reisishot.mise.backend.config.category.CategoryInformation
import pictures.reisishot.mise.backend.config.category.flatten
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator.Companion.LINKTYPE_CATEGORIES
import pictures.reisishot.mise.backend.generator.gallery.insertCategoryThumbnails
import pictures.reisishot.mise.backend.generator.gallery.insertImageGallery
import pictures.reisishot.mise.backend.generator.gallery.insertSubcategoryThumbnails
import pictures.reisishot.mise.backend.html.appendUnformattedHtml
import pictures.reisishot.mise.backend.html.config.TemplateObject
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import pictures.reisishot.mise.commons.ComplexName

@WebsiteConfigBuilderDsl
fun AbstractGalleryGenerator.createCategoryApi(): Pair<String, VelocityTemplateObjectCreator> =
    "categories" to { _, websiteConfig, buildingCache ->
        CategoryApi(this, websiteConfig, buildingCache)
    }

internal class CategoryApi(
    private val galleryGenerator: AbstractGalleryGenerator,
    private val configuration: WebsiteConfig,
    private val buildingCache: BuildingCache,
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

    @JvmOverloads
    fun insertImagesForCategory(albumName: String, limit: Int = -1) = buildString {
        val category = findAlbumsWithName(setOf(albumName))
            .firstOrNull() ?: throw IllegalStateException("An album with name \"$albumName\" has not been found!")
        appendUnformattedHtml().div {
            insertImageGallery(galleryGenerator, configuration, category.images, limit = limit)

            if (limit > 0 && category.images.size > limit)
                div("center") {
                    a(
                        buildingCache.getLinkcacheEntryFor(configuration, LINKTYPE_CATEGORIES, albumName.lowercase()),
                        classes = "pl-2 btn btn-primary center"
                    ) {
                        text("Alle Bilder ansehen")
                    }
                }
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
