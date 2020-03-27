package pictures.reisishot.mise.configui

import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.*
import java.nio.file.Path
import java.util.function.Consumer

class FilenameChooser : HBox(5.0), Consumer<Path> {

    private val input = AutocompleteMultiSelectionBox<FilenameData>(2) { null }

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
                    input.accept(it)
                }
            }
        }
    }

    override fun accept(t: Path) {
        FilenameData.fromPath(t)?.let { input.accept(it) }
    }

    private fun AutocompleteMultiSelectionBox<FilenameData>.accept(filenameData: FilenameData) {
        with(input.suggestions) {
            if (!contains(filenameData))
                add(filenameData)
        }
        selectedItems = listOf(filenameData)
    }
}