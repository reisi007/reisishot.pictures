package pictures.reisishot.mise.backend.generator.pages.minimalistic

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.IPageMininmalInfo
import pictures.reisishot.mise.backend.generator.pages.PageGeneratorExtension
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.getString
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists

@ExperimentalPathApi
class MinimalisticPageGenerator(private val galleryGenerator: AbstractGalleryGenerator) : PageGeneratorExtension {

    override fun processDelete(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: Path) {
        targetPath.minimalDestinationPath.deleteIfExists()
    }


    override fun postCreatePage(configuration: WebsiteConfiguration, cache: BuildingCache, pageMinimalInfo: IPageMininmalInfo, yaml: Yaml, content: String) {
        if (yaml.getString("minimal").toBoolean()) {
            buildPage(pageMinimalInfo, configuration, cache, content)
        }
    }

    private fun buildPage(
            pageMinimalInfo: IPageMininmalInfo,
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            content: String
    ) {

        PageGenerator.generatePage(
                pageMinimalInfo.targetPath.minimalDestinationPath,
                pageMinimalInfo.title,
                websiteConfiguration = configuration,
                buildingCache = cache,
                galleryGenerator = galleryGenerator,
                minimalPage = true,
        ) {
            raw(content)
        }
    }

    private val Path.minimalDestinationPath: Path
        get() = resolveSibling("minimal.html")
}

typealias SourcePath = Path
typealias TargetPath = Path
typealias Yaml = Map<String, List<String>>