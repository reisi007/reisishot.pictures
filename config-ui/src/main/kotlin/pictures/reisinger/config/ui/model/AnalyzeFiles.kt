package pictures.reisinger.config.ui.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import pictures.reisishot.mise.commons.fileExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.commons.isJpeg
import pictures.reisishot.mise.commons.isJson
import pictures.reisishot.mise.commons.isRegularFile
import pictures.reisishot.mise.commons.list
import pictures.reisishot.mise.config.ImageConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.math.max

fun Path.findUsedFilenames() = list()
    .mapNotNull { it.toFileInfo() }
    .groupingBy { it.displayName }
    .fold({ _, v -> v.digitCount }, { _, current, v -> max(current, v.digitCount) })
    .asSequence()
    .map { (k, v) -> FilenameInfo(k, v) }
    .sortedBy { it.displayName }
    .toList()

fun Path.findMissingFiles() = list()
    .filter { it.fileExtension.isJpeg() }
    .filter { !it.resolveSibling(it.filenameWithoutExtension + ".json").exists() }
    .map { it to ImmutableImageConfig() }
    .toList()

fun Path.findAllTags() = list()
    .filter { it.fileExtension.isJson() }
    .mapNotNull { it.fromJson<ImageConfig>() }
    .flatMap { it.tags }
    .sorted()
    .distinct()
    .toList()

fun Iterable<Path>.readExistingConfigFiles(): List<Pair<Path, ImmutableImageConfig>> = mapNotNull { configPath ->
    configPath.withNewExtension("json")
        .fromJson<ImmutableImageConfig>()
        ?.let { configPath to it }
}

fun Path.withNewExtension(newExtension: String): Path = resolveSibling("$filenameWithoutExtension.$newExtension")

val JSON by lazy {
    Json {
        allowStructuredMapKeys = true
    }
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> T.toJson(path: Path) {
    path.parent?.let {
        Files.createDirectories(it)
        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .use { os ->
                JSON.encodeToStream(this, os)
            }
    }
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Path.fromJson(): T? =
    if (!(exists() && isRegularFile())) null else
        Files.newInputStream(this).use { reader ->
            JSON.decodeFromStream(reader)
        }
