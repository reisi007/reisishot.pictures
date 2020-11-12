package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.FileExtension
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.htmlparsing.TargetPath
import pictures.reisishot.mise.backend.htmlparsing.Yaml

interface YamlMetaDataConsumer {

    fun init(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    fun processFrontMatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml): HEAD.() -> Unit

    fun processDelete(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath) {
    }

    fun processChanges(configuration: WebsiteConfiguration, cache: BuildingCache) {
    }

    fun cleanupArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    fun interestingFileExtensions(): Sequence<(FileExtension) -> Boolean> = emptySequence()
}