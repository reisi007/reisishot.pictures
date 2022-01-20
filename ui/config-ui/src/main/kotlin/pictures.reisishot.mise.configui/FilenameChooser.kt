package pictures.reisishot.mise.configui

import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import pictures.reisishot.mise.base.AutocompleteMultiSelectionBox
import tornadofx.*
import java.nio.file.Path
import java.util.function.Consumer

class FilenameChooser : HBox(5.0), Consumer<Path> {

    private val input = AutocompleteMultiSelectionBox(2) { FilenameData(it) }

    var items: MutableCollection<FilenameData> = input.suggestions

    var selectedItems: List<FilenameData>
        get() = input.tags
        set(value) {
            with(input.tags) {
                clear()
                addAll(value)
            }
        }


    init {
        vgrow = Priority.NEVER
        hgrow = Priority.ALWAYS
        alignment = Pos.CENTER
        add(input)
        button("Neuer Name") {
            setOnAction {
                NewFilenameDialog().showAndWait().ifPresent {
                    accept(it)
                }
            }
        }
    }

    override fun accept(t: Path) {
        accept(FilenameData.fromPath(t))
    }

    private fun accept(filenameData: FilenameData) {
        with(input.suggestions) {
            add(filenameData)
        }
        selectedItems = listOf(filenameData)
    }
}
