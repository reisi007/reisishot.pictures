package pictures.reisishot.mise.backend.generator.pages.minimalistic

import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.PageGeneratorExtension
import pictures.reisishot.mise.backend.htmlparsing.Yaml
import pictures.reisishot.mise.backend.htmlparsing.getString
import java.nio.file.Files
import java.nio.file.Path

class MinimalisticPageGenerator : PageGeneratorExtension {

    override fun processDelete(configuration: WebsiteConfig, cache: BuildingCache, targetPath: Path) {
        Files.deleteIfExists(targetPath.minimalDestinationPath)
    }

    override fun postCreatePage(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMinimalInfo: IPageMinimalInfo,
        yaml: Yaml,
        content: String
    ) {
        if (yaml.getString("minimal").toBoolean()) {
            buildPage(pageMinimalInfo, configuration, cache, content)
        }
    }

    private fun buildPage(
        pageMinimalInfo: IPageMinimalInfo,
        configuration: WebsiteConfig,
        cache: BuildingCache,
        content: String
    ) {
        PageGenerator.generatePage(
            pageMinimalInfo.targetPath.minimalDestinationPath,
            pageMinimalInfo.title,
            websiteConfig = configuration,
            buildingCache = cache,
            isMinimalPage = true,
        ) {
            raw(content)
        }
    }

    private val Path.minimalDestinationPath: Path
        get() = resolveSibling("minimal.html")
}
