package pictures.reisishot.mise.backend.generator.pages.yamlConsumer

import at.reisishot.mise.backend.config.BuildingCache
import at.reisishot.mise.backend.config.WebsiteConfig
import kotlinx.html.HEAD
import kotlinx.html.meta
import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.htmlparsing.PageGeneratorExtension
import pictures.reisishot.mise.backend.htmlparsing.Yaml

class KeywordConsumer : PageGeneratorExtension {

    override fun processFrontmatter(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMinimalInfo: IPageMinimalInfo,
        frontMatter: Yaml
    ): HEAD.() -> Unit {
        return {
            frontMatter["keywords"]?.joinToString(",")?.let {
                meta("keywords", it)
            }
        }
    }
}
