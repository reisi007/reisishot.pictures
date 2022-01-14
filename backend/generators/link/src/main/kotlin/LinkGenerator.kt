package pictures.reisishot.mise.backend.generator.links

import at.reisishot.mise.backend.config.*
import kotlinx.serialization.Serializable
import kotlin.io.path.exists

class LinkGenerator : WebsiteGenerator {

    override val generatorName: String = "Link Generator"

    companion object {
        const val LINK_TYPE = "MANUAL"
        const val FILENAME = "urls.json"
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) = configuration.useJsonParserParallel {
        val configFile = configuration.getConfigFile()
        if (configFile.exists()) {
            val data = configFile.fromJson<List<ManualLink>>() ?: emptyList<ManualLink>()
            if (data.isNotEmpty()) {
                cache.clearMenuItems { it.id.startsWith(LINK_TYPE) }
                data.forEach { (name, index, value, target) ->
                    val url = value.let {
                        if (value.startsWith("/"))
                            it.substringAfter("/")
                        else it
                    }
                    cache.addMenuItem(LINK_TYPE + "_" + name, index, url, name, target)
                }
            }
        }
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        val configFile = configuration.getConfigFile()
        if (changeFiles.keys.any(configFile::equals)) {
            fetchInitialInformation(configuration, cache, alreadyRunGenerators)
            return true
        } else return false
    }

    private fun WebsiteConfig.getConfigFile() =
        paths.sourceFolder.resolve(FILENAME)

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, cache: BuildingCache) {
        // No action needed
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        // No action needed
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfig, cache: BuildingCache) {
        // No action needed
    }
}

@Serializable
data class ManualLink(val name: String, val index: Int, val value: String, val target: String? = null)
