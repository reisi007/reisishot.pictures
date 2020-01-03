package pictures.reisishot.mise.configui

import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.*
import java.nio.file.Path
import java.util.function.Consumer

class FilenameChooser : HBox(5.0), Consumer<Path> {

    val items = FXCollections.observableList(ArrayList<FilenameData>())
    private val comboBox: ComboBox<FilenameData> = ComboBox(items.sorted())

    val selectedItem
        get() = comboBox.value ?: throw IllegalStateException("No filename pattern selected!")


    init {
        vgrow = Priority.NEVER
        hgrow = Priority.ALWAYS
        alignment = Pos.CENTER
        add(comboBox)
        button("Neuer Name") {
            setOnAction {
                NewFilenameDialog().showAndWait().ifPresent {
                    comboBox.accept(it)
                }
            }
        }
    }

    override fun accept(t: Path) {
        FilenameData.fromPath(t)?.let { comboBox.accept(it) }
    }

    private fun ComboBox<FilenameData>.accept(filenameData: FilenameData) {
        with(this@FilenameChooser.items) {
            if (!contains(filenameData))
                add(filenameData)
        }
        value = filenameData
    }
}