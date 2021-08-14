package pictures.reisishot.mise.backend.generator.copy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CopyGenerator(private val matchRegex: List<Regex>) : WebsiteGenerator {
    override val generatorName: String = "Copy Files"

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        val inPath = configuration.inPath
        val outPath = configuration.outPath

        withContext(Dispatchers.IO) {
            Files.walk(inPath)
                .filter {
                    val filename = it.fileName.toString()
                    matchRegex.any { regex -> regex.matches(filename) }
                }.forEach {
                    Files.copy(it, outPath.resolve(it.relativize(inPath)), StandardCopyOption.REPLACE_EXISTING)
                }
        }
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        updateId: Long,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        changeFiles
            .filter { (p, _) ->
                val filename = p.fileName.toString()
                matchRegex.any { regex -> regex.matches(filename) }
            }
            .map { (k, v) -> Pair(k, v) }
            .forEach { changedFiles ->
                val (path) = changedFiles
                val relativePath = path.relativize(configuration.inPath)
                withContext(Dispatchers.IO) {
                    if (changedFiles.isStateDeleted()) {
                        Files.deleteIfExists(configuration.outPath.resolve(relativePath))
                    } else if (changedFiles.isStateEdited()) {
                        Files.copy(
                            path,
                            configuration.outPath.resolve(relativePath),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }
                }
            }
        return false
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        updateId: Long,
        changeFiles: ChangeFileset
    ): Boolean {
        // Nothing to do
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        withContext(Dispatchers.IO) {
            Files.walk(configuration.outPath)
                .filter {
                    val filename = it.fileName.toString()
                    matchRegex.any { regex -> regex.matches(filename) }
                }.forEach { Files.deleteIfExists(it) }
        }
    }

}
