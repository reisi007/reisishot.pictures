package pictures.reisinger.config.ui.model

import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import java.nio.file.Path
import kotlin.io.path.exists

open class FilenameInfo(val displayName: FilenameWithoutExtension, val digitCount: Int) {
    override fun toString() = displayName
}

private val REGEX_SPLIT_FILEINFO = """(.+?)(\d+)$""".toRegex()
fun Path.toFileInfo(): FilenameInfo? = REGEX_SPLIT_FILEINFO.matchEntire(filenameWithoutExtension)?.let {
    val (filename, count) = it.destructured
    FilenameInfo(filename, count.length)
}

fun FilenameInfo.buildFilenameWithoutExtension(cnt: Int): FilenameWithoutExtension =
    displayName + cnt.toString().padStart(digitCount, ' ')

fun FilenameInfo.nextFreeFilename(folder: Path, extension: String): Path {
    var cnt = 1
    while (true) {
        val cur = buildFilenameWithoutExtension(cnt) + "." + extension
        val curPath = folder.resolve(cur)
        if (!curPath.exists()) {
            return curPath
        }
        cnt++;
    }
}
