package pictures.reisishot.mise.backend.generator.pages.yamlConsumer;

import kotlinx.html.HEAD
import kotlinx.html.meta
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
import pictures.reisishot.mise.backend.htmlparsing.TargetPath
import pictures.reisishot.mise.backend.htmlparsing.Yaml

class KeywordConsumer : YamlMetaDataConsumer {

    override fun processFrontmatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml): HEAD.() -> Unit {
        return {
            frontMatter["keywords"]?.joinToString(",")?.let {
                meta("keywords", it)
            }
        }
    }
}
