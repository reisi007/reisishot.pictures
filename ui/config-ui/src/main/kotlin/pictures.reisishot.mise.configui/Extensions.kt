package pictures.reisishot.mise.configui

import at.reisishot.mise.commons.exists
import javafx.beans.property.ReadOnlyDoubleProperty
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path

operator fun Int.times(value: ReadOnlyDoubleProperty) = value * this
operator fun Double.times(value: ReadOnlyDoubleProperty) = value * this

fun FilenameData.getNextFreePath(original: Path): Path {
    if (Files.isDirectory(original))
        throw IllegalStateException("File expected!")
    var newPath: Path
    var number = 1
    do {
        newPath = original.resolveSibling(name + number.toString().padStart(digitCount, '0') + ".jpg")
        number++
    } while (newPath.exists())
    return newPath
}