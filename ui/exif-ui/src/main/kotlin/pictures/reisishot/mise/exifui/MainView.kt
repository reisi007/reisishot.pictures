package pictures.reisishot.mise.exifui

import at.reisishot.mise.commons.FileExtension
import at.reisishot.mise.commons.hasExtension
import at.reisishot.mise.commons.isJpeg
import at.reisishot.mise.exifdata.ExifdataKey
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import at.reisishot.mise.exifdata.readExif
import javafx.event.EventHandler
import javafx.scene.input.TransferMode
import tornadofx.*

class MainView : View("Exif Extractor") {


    override val root = textarea {
        isEditable = false

        onDragOver = EventHandler {
            if (it.gestureSource != this && it.dragboard.hasFiles()) {
                /* allow for both copying and moving, whatever user chooses */
                it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            }
            it.consume()
        }

        onDragDropped = EventHandler { event ->
            if (event.dragboard.hasFiles()) {
                text = event.dragboard.files.asSequence()
                    .map { it.toPath() }
                    .filter { it.hasExtension(FileExtension::isJpeg) }
                    .map { it.readExif(defaultExifReplaceFunction) }
                    .map {
                        var make = it[ExifdataKey.CAMERA_MAKE]
                        val model = it[ExifdataKey.CAMERA_MODEL]
                        if (make != null && model != null && model.startsWith(make))
                            make = null
                        sequenceOf(
                            make,
                            model,
                            it[ExifdataKey.LENS_MODEL],
                            it[ExifdataKey.FOCAL_LENGTH]?.substringBefore(",0")?.replace(',', '.'),
                            it[ExifdataKey.APERTURE],
                            it[ExifdataKey.SHUTTER_SPEED],
                            it[ExifdataKey.ISO]?.let { "ISO $it" }
                        ).filterNotNull()
                            .joinToString(" | ")
                    }.joinToString(System.lineSeparator())
            }
            event.consume()
        }
    }
}

private fun <T> MutableList<T>.addNonNull(t: T?) {
    t?.let { add(it) }
}
