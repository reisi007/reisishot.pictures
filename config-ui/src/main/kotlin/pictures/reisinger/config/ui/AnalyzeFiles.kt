package pictures.reisinger.config.ui

import pictures.reisishot.mise.commons.fileExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.commons.isJpeg
import pictures.reisishot.mise.commons.list
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.math.max

fun Path.findUsedFilenames() = list()
    .mapNotNull { it.toFileInfo() }
    .groupingBy { it.displayName }
    .fold({ _, v -> v.digitCount }, { _, current, v -> max(current, v.digitCount) })
    .asSequence()
    .map { (k, v) ->
        FilenameInfo(k, v)
    }
    .sortedBy { it.displayName }
    .toList()

fun Path.findMissingFiles() = list()
    .filter { it.fileExtension.isJpeg() }
    .filter { !it.resolveSibling(it.filenameWithoutExtension + ".json").exists() }
    .map { it to setOf<String>() }
    .toList()
