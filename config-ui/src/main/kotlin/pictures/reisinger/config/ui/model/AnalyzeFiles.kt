package pictures.reisinger.config.ui.model

import pictures.reisishot.mise.commons.fileExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.commons.isJpeg
import pictures.reisishot.mise.commons.isJson
import pictures.reisishot.mise.commons.isRegularFile
import pictures.reisishot.mise.commons.list
import pictures.reisishot.mise.commons.withNewExtension
import pictures.reisishot.mise.config.ImageConfig
import pictures.reisishot.mise.json.fromJson
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.math.max

fun Path.findUsedFilenames() = list()
    .filter { it.isRegularFile() && it.fileExtension.isJpeg() }
    .mapNotNull { it.toFileInfo() }
    .groupingBy { it.displayName }
    .fold({ _, v -> v.digitCount }, { _, current, v -> max(current, v.digitCount) })
    .asSequence()
    .map { (k, v) -> FilenameInfo(k, v) }
    .sortedBy { it.displayName }
    .toList()

fun Path.findMissingFiles() = list()
    .filter { it.isRegularFile() && it.fileExtension.isJpeg() }
    .filter { !it.resolveSibling(it.filenameWithoutExtension + ".json").exists() }
    .map { it to ImmutableImageConfig() }
    .toList()

fun Path.findAllTags() = list()
    .filter { it.isRegularFile() && it.fileExtension.isJson() && it.withNewExtension("jpg").exists() }
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
