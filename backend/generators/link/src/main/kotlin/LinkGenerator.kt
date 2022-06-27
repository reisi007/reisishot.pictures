package pictures.reisishot.mise.backend.generator.links

import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.ChangeFileset
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteGenerator
import pictures.reisishot.mise.backend.config.useJsonParserParallel
import kotlin.io.path.exists

class LinkGenerator : WebsiteGenerator {

    override val generatorName: String = "Link Generator"

    companion object {
        const val LINK_TYPE = "MANUAL"
        const val FILENAME = "urls.json"
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) = configuration.useJsonParserParallel {
        val configFile = configuration.getConfigFile()
        if (configFile.exists()) {
            val data = configFile.fromJson<List<ManualLink>>() ?: emptyList()
            if (data.isNotEmpty()) {
                buildingCache.clearMenuItems { it.id.startsWith(LINK_TYPE) }
                data.forEach { (name, index, value, target) ->
                    val url = value.let {
                        if (value.startsWith("/"))
                            it.substringAfter("/")
                        else it
                    }
                    buildingCache.addMenuItem(LINK_TYPE + "_" + name, index, url, name, target)
                }
            }
        }
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        val configFile = configuration.getConfigFile()
        return if (changeFiles.keys.any(configFile::equals)) {
            fetchInitialInformation(configuration, buildingCache, alreadyRunGenerators)
            true
        } else false
    }

    private fun WebsiteConfig.getConfigFile() =
        paths.sourceFolder.resolve(FILENAME)

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        // No action needed
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        // No action needed
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        // No action needed
    }
}

@Serializable
data class ManualLink(val name: String, val index: Int, val value: String, val target: String? = null)
