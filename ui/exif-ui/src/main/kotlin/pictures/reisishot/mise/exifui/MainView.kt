package pictures.reisishot.mise.exifui

import javafx.event.EventHandler
import javafx.scene.input.TransferMode
import pictures.reisishot.mise.commons.FileExtension
import pictures.reisishot.mise.commons.hasExtension
import pictures.reisishot.mise.commons.isJpeg
import pictures.reisishot.mise.exifdata.ExifdataKey.*
import pictures.reisishot.mise.exifdata.defaultExifReplaceFunction
import pictures.reisishot.mise.exifdata.readExif
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
                    .map { exifData ->
                        var make = exifData[CAMERA_MAKE]
                        val model = exifData[CAMERA_MODEL]
                        if (make != null && model != null && model.startsWith(make))
                            make = null
                        sequenceOf(
                            make,
                            model,
                            exifData[LENS_MODEL],
                            exifData[FOCAL_LENGTH]?.substringBefore(",0")?.replace(',', '.'),
                            exifData[APERTURE],
                            exifData[SHUTTER_SPEED],
                            exifData[ISO]?.let { "ISO $it" }
                        ).filterNotNull()
                            .joinToString(" | ")
                    }.joinToString(System.lineSeparator())
            }
            event.consume()
        }
    }
}
