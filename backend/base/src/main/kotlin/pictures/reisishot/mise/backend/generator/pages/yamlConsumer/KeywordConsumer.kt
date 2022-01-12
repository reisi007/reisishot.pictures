package pictures.reisishot.mise.backend.generator.pages.yamlConsumer

import kotlinx.html.HEAD
import kotlinx.html.meta
import pictures.reisishot.mise.backend.BuildingCache
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.pages.IPageMininmalInfo
import pictures.reisishot.mise.backend.generator.pages.PageGeneratorExtension
import pictures.reisishot.mise.backend.generator.pages.Yaml

class KeywordConsumer : PageGeneratorExtension {

    override fun processFrontmatter(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        pageMinimalInfo: IPageMininmalInfo,
        frontMatter: Yaml
    ): HEAD.() -> Unit {
        return {
            frontMatter["keywords"]?.joinToString(",")?.let {
                meta("keywords", it)
            }
        }
    }
}
