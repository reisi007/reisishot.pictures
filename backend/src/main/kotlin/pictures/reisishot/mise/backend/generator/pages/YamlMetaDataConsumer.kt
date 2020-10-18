package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.FileExtension
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.htmlparsing.TargetPath
import pictures.reisishot.mise.backend.htmlparsing.Yaml

interface YamlMetaDataConsumer {

    fun init(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    fun processFrontMatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml, galleryGenerator: AbstractGalleryGenerator): HEAD.() -> Unit

    fun processDelete(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath) {
    }

    fun processChanges(configuration: WebsiteConfiguration, cache: BuildingCache, galleryGenerator: AbstractGalleryGenerator, metaDataConsumers: Array<out YamlMetaDataConsumer>) {
    }

    fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    fun interestingFileExtensions(): Sequence<(FileExtension) -> Boolean> = emptySequence()
}