package pictures.reisishot.mise.configui

import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import pictures.reisishot.mise.base.AutocompleteMultiSelectionBox
import tornadofx.*

class FilenameChooser : HBox(5.0) {

    val input = AutocompleteMultiSelectionBox<FilenameData>(2)

    init {
        vgrow = Priority.NEVER
        hgrow = Priority.ALWAYS
        alignment = Pos.CENTER
        add(input)
        button("Neuer Name") {
            setOnAction {
                NewFilenameDialog().showAndWait().ifPresent {
                    input.chooser.sourceItems.add(it)
                }
            }
        }
    }


}