package pictures.reisishot.mise.backend.generator.links

import at.reisishot.mise.commons.exists
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.BuildingCache
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.parser

class LinkGenerator : WebsiteGenerator {

    override val generatorName: String = "Link Generator"

    companion object {
        const val LINK_TYPE = "MANUAL"
        const val FILENAME = "urls.json"
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) = with(configuration.parser) {
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
        configuration: WebsiteConfiguration,
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

    private fun WebsiteConfiguration.getConfigFile() =
        inPath.resolve(FILENAME)

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // No action needed
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        // No action needed
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // No action needed
    }
}

@Serializable
data class ManualLink(val name: String, val index: Int, val value: String, val target: String? = null)
