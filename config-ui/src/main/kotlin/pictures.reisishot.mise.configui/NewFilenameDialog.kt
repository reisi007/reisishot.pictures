package pictures.reisishot.mise.configui

import javafx.application.Platform
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.util.Callback
import tornadofx.*

class NewFilenameDialog : Dialog<FilenameData?>() {

    private val nameField = textfield("")
    private val numberField = combobox(values = (1..10).toList()).apply {
        value = 3
    }
    private val okButton = ButtonType("Erstellen", ButtonBar.ButtonData.OK_DONE)

    init {
        title = "Neuen Namen eingeben"
        headerText = "Bitte geben Sie die benötigten Informationen ein:"

        dialogPane.buttonTypes.addAll(
                okButton,
                ButtonType.CANCEL
        )

        dialogPane.content = form {
            fieldset {
                field("Name") {
                    add(nameField)
                }
                field("Anzahl Stellen Nummer") {
                    add(numberField)
                }
            }
        }

        resultConverter = Callback<ButtonType, FilenameData?> {
            if (it != okButton)
                null
            else FilenameData(
                    nameField.text.trim().let { if (it.isEmpty()) throw IllegalStateException("Name not selected") else it },
                    numberField.selectedItem ?: throw IllegalStateException("Item count not selected")
            )
        }

        Platform.runLater { nameField.requestFocus() }
    }
}