package pictures.reisishot.mise.configui

import at.reisishot.mise.commons.filenameWithoutExtension
import java.nio.file.Path

data class FilenameData(val name: String, val digitCount: Int = 3) : Comparable<FilenameData> {
    override fun toString(): String {
        return name
    }

    companion object {
        private val nameSplittingPattern = """^(?<name>.*?)(?<count>\d+)$""".toRegex()
        fun fromPath(p: Path) =
                nameSplittingPattern.matchEntire(p.filenameWithoutExtension)?.let { result ->
                    val (name, numberPart) = result.destructured
                    FilenameData(name, numberPart.length)
                } ?: FilenameData(p.filenameWithoutExtension)
    }

    override fun compareTo(other: FilenameData): Int = compareValuesBy(this, other, FilenameData::name)
}