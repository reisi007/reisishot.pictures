package pictures.reisishot.mise.backend.generator.pages

import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache

interface YamlMetaDataConsumer {

    fun init(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    fun processFrontMatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml, host: PageGenerator): HEAD.() -> Unit

    fun processDelete(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath) {
    }

    fun processChanges(configuration: WebsiteConfiguration, cache: BuildingCache, host: pictures.reisishot.mise.backend.generator.pages.PageGenerator) {
    }

    fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }
}