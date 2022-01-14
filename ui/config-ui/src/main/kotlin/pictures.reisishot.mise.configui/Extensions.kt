package pictures.reisishot.mise.configui

import javafx.beans.property.ReadOnlyDoubleProperty
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

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
