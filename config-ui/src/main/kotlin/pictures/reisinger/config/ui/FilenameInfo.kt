package pictures.reisinger.config.ui

import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import java.nio.file.Path

open class FilenameInfo(val displayName: FilenameWithoutExtension, val digitCount: Int) {
    override fun toString() = displayName
}

private val REGEX_SPLIT_FILEINFO = """(.+?)(\d+)$""".toRegex()
fun Path.toFileInfo(): FilenameInfo? = REGEX_SPLIT_FILEINFO.matchEntire(filenameWithoutExtension)?.let {
    val (filename, count) = it.destructured
    FilenameInfo(filename, count.length)
}
