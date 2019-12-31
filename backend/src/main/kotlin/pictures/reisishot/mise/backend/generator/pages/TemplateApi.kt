package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.commons.filenameWithoutExtension
import at.reisishot.mise.commons.toArray
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.insertCategoryThumbnails
import pictures.reisishot.mise.backend.generator.gallery.insertSubcategoryThumbnails
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.insertLazyPicture

class TemplateApi(
        private val targetPath: TargetPath,
        private val galleryGenerator: AbstractGalleryGenerator,
        private val cache: BuildingCache,
        private val websiteConfiguration: WebsiteConfiguration
) {
    private var privateHasGallery = false
    val hasGallery
        get() = privateHasGallery


    private fun Map<FilenameWithoutExtension, InternalImageInformation>.getOrThrow(key: FilenameWithoutExtension) =
            this[key]
                    ?: throw IllegalStateException("Cannot find picture with filename \"$key\" (used in ${targetPath.filenameWithoutExtension})!")

    @SuppressWarnings("unused")
    fun insertPicture(filenameWithoutExtension: FilenameWithoutExtension) = buildString {
        appendHTML(prettyPrint = false, xhtmlCompatible = true).div {
            with(galleryGenerator.cache) {
                insertLazyPicture(imageInformationData.getOrThrow(filenameWithoutExtension))
            }
        }
    }

    @SuppressWarnings("unused")
    fun insertGallery(
            galleryName: String,
            vararg filenameWithoutExtension: FilenameWithoutExtension
    ): String {
        privateHasGallery = privateHasGallery || filenameWithoutExtension.isNotEmpty()
        return with(galleryGenerator.cache) {
            filenameWithoutExtension.asSequence()
                    .map {
                        imageInformationData.getOrThrow(it)
                    }.toArray(filenameWithoutExtension.size).let { imageInformations ->
                        kotlin.text.buildString {
                            appendHTML(prettyPrint = false, xhtmlCompatible = true).div {
                                insertImageGallery(galleryName, *imageInformations)
                            }
                        }
                    }
        }
    }

    @SuppressWarnings("unused")
    fun insertLink(type: String, key: String): String = websiteConfiguration.websiteLocation + cache.getLinkcacheEntryFor(type, key)

    @SuppressWarnings("unused")
    fun insertLink(linktext: String, type: String, key: String): String = buildString {
        appendHTML(false, true).a(insertLink(type, key)) {
            text(linktext)
        }
    }

    @SuppressWarnings("unused")
    fun insertSubalbumThumbnails(albumName: String?): String = buildString {
        appendHTML(false, true).div {
            insertSubcategoryThumbnails(CategoryName(albumName ?: ""), galleryGenerator)
        }
    }

    @SuppressWarnings("unused")
    fun insertCategoryOverview(vararg albumName: String) = buildString {
        if (albumName.isEmpty()) return@buildString
        appendHTML(false, true).div {
            val albums = albumName.asSequence()
                    .map { CategoryName(it) }
                    .toCollection(LinkedHashSet())
            insertCategoryThumbnails(albums, galleryGenerator);
        }
    }
}