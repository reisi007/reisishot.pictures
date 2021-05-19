package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.FileExtension
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.pages.minimalistic.Yaml
import java.nio.file.Path

interface PageGeneratorExtension {

    fun init(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    fun processFrontmatter(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        pageMinimalInfo: IPageMininmalInfo,
        frontMatter: Yaml
    ): HEAD.() -> Unit {
        return {}
    }

    fun processDelete(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: Path) {
    }

    fun processChanges(configuration: WebsiteConfiguration, cache: BuildingCache) {
    }

    fun cleanupArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    fun postCreatePage(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        pageMinimalInfo: IPageMininmalInfo,
        yaml: Yaml,
        content: String
    ) {

    }

    fun interestingFileExtensions(): Sequence<(FileExtension) -> Boolean> = emptySequence()
}
