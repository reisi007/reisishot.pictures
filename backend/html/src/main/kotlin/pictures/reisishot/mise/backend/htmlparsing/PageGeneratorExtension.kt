package pictures.reisishot.mise.backend.htmlparsing

import at.reisishot.mise.backend.config.BuildingCache
import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.commons.FileExtension
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.IPageMinimalInfo
import java.nio.file.Path

interface PageGeneratorExtension {

    fun init(configuration: WebsiteConfig, cache: BuildingCache) {
        // Nothing to do
    }

    fun processFrontmatter(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMinimalInfo: IPageMinimalInfo,
        frontMatter: Yaml
    ): HEAD.() -> Unit {
        return {}
    }

    fun processDelete(configuration: WebsiteConfig, cache: BuildingCache, targetPath: Path) {
    }

    fun processChanges(configuration: WebsiteConfig, cache: BuildingCache) {
    }

    fun cleanupArtifacts(configuration: WebsiteConfig, cache: BuildingCache) {
        // Nothing to do
    }

    fun postCreatePage(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMinimalInfo: IPageMinimalInfo,
        yaml: Yaml,
        content: String
    ) {

    }

    fun interestingFileExtensions(): Sequence<(FileExtension) -> Boolean> = emptySequence()
}
