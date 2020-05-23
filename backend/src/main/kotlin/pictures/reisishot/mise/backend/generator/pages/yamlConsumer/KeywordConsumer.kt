package pictures.reisishot.mise.backend.generator.pages.yamlConsumer;

import kotlinx.html.HEAD
import kotlinx.html.meta
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.pages.TargetPath
import pictures.reisishot.mise.backend.generator.pages.Yaml
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer

class KeywordConsumer : YamlMetaDataConsumer {

    override fun processFrontMatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml, host: PageGenerator): HEAD.() -> Unit {
        return {
            frontMatter["keywords"]?.joinToString(", ")?.let {
                meta("keywords", it)
            }
        }
    }
}
