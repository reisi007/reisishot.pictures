package pictures.reisishot.mise.configui

import javafx.collections.FXCollections
import javafx.collections.ListChangeListener.Change
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ContextMenu
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import java.util.*

/**
 * Code based on https://stackoverflow.com/a/56644865/1870799 with minor modifications
 */
class AutocompleteMultiSelectionBox : HBox() {
    val tags: ObservableList<String>
    val suggestions: ObservableSet<String>
    private val entriesPopup: ContextMenu
    private val inputTextField: TextField
    /**
     * "Suggestion" specific listners
     */
    private fun setListner() {
        //Add "suggestions" by changing text
        inputTextField.textProperty().addListener { _, _, newValue ->
            //always hide suggestion if nothing has been entered (only "spacebars" are disalowed in TextFieldWithLengthLimit)
            if (newValue.isEmpty()) {
                entriesPopup.hide()
            } else {
                //filter all possible suggestions depends on "Text", case insensitive
                val filteredEntries: List<String> = suggestions.asSequence()
                        .filter { it.contains(newValue, true) }
                        .toList()
                //some suggestions are found
                if (filteredEntries.isNotEmpty()) {
                    //build popup - list of "CustomMenuItem"
                    populatePopup(filteredEntries, newValue)
                    if (!entriesPopup.isShowing) { //optional
                        entriesPopup.show(this, Side.BOTTOM, 0.0, 0.0) //position of popup
                    }
                    //no suggestions -> hide

                } else {
                    entriesPopup.hide()
                }
            }
        }

        //Hide always by focus-in (optional) and out
        focusedProperty().addListener { _, _, _ ->
            entriesPopup.hide()
        }
    }

    /**
     * Populate the entry set with the given search results. Display is limited to 10 entries, for performance.
     *
     * @param searchResult The set of matching strings.
     */
    private fun populatePopup(searchResult: List<String>, searchRequest: String) {
        //List of "suggestions"
        //Build list as set of labels
        val menuItems = searchResult.asSequence()
                .take(MAX_ENTRIES)// Limit to MAX_ENTRIES in the suggestions
                .minus(tags)
                .map { result: String ->
                    //label with graphic (text flow) to highlight founded subtext in suggestions
                    val textFlow = buildTextFlow(result, searchRequest)
                    textFlow.prefWidthProperty().bind(widthProperty())
                    val item = CustomMenuItem(textFlow, true)

                    //if any suggestion is select set it into text and close popup
                    item.onAction = EventHandler { actionEvent: ActionEvent ->
                        tags.add(result)
                        suggestions.remove(result)
                        inputTextField.clear()
                        entriesPopup.hide()
                    }
                    item
                }.toList()

        //"Refresh" context menu
        entriesPopup.items.clear()
        entriesPopup.items.addAll(menuItems)
    }

    /**
     * Clears then repopulates the entries with the new set of data.
     *
     * @param suggestions set of items.
     */

    fun setSuggestions(suggestions: ObservableSet<String?>) {
        this.suggestions.clear()
        this.suggestions.addAll(suggestions)
    }

    private inner class Tag internal constructor(tag: String) : HBox() {
        init {
            // Style
            styleClass.add("tag")
            // Remove item button
            val removeButton = Button("x")
            removeButton.background = null
            removeButton.onAction = EventHandler { event: ActionEvent ->
                tags.remove(tag)
                suggestions.add(tag)
                inputTextField.requestFocus()
            }

            // Displayed text
            val text = Text(tag)
            text.fill = Color.WHITE
            text.font = Font.font(text.font.family, FontWeight.BOLD, text.font.size)

            // Children position
            alignment = Pos.CENTER
            spacing = 5.0
            padding = Insets(0.0, 0.0, 0.0, 5.0)
            children.addAll(text, removeButton)
        }
    }

    companion object {
        private const val MAX_ENTRIES = 10
        /**
         * Build TextFlow with selected text. Return "case" dependent.
         *
         * @param text   - string with text
         * @param filter - string to select in text
         * @return - TextFlow
         */
        private fun buildTextFlow(text: String, filter: String): TextFlow {
            val filterIndex = text.toLowerCase().indexOf(filter.toLowerCase())
            val textBefore = Text(text.substring(0, filterIndex))
            val textAfter = Text(text.substring(filterIndex + filter.length))
            val textFilter = Text(text.substring(filterIndex,
                    filterIndex + filter.length)) //instead of "filter" to keep all "case sensitive"

            textFilter.fill = Color.ORANGE
            textFilter.font = Font.font("Helvetica", FontWeight.BOLD, 12.0)
            return TextFlow(textBefore, textFilter, textAfter)
        }
    }

    init {
        styleClass.setAll("tag-bar")
        ClassLoader.getSystemClassLoader().getResource("style.css")?.toExternalForm()?.let {
            stylesheets.add(it)
        } ?: throw IllegalStateException("No styles found")

        tags = FXCollections.observableArrayList()
        suggestions = FXCollections.observableSet()
        inputTextField = TextField()
        entriesPopup = ContextMenu()
        setListner()
        inputTextField.onKeyPressed = EventHandler { event: KeyEvent ->
            // Remove last element with backspace
            if (event.code == KeyCode.BACK_SPACE && tags.isNotEmpty() && inputTextField.text.isEmpty()) {
                val last: String = tags.last()
                suggestions.add(last)
                tags.remove(last)
            }

        }
        inputTextField.onKeyTyped = EventHandler { event ->
            if (event.character == "\r" && inputTextField.text.isNotEmpty()) {
                val newTag = inputTextField.text
                suggestions.add(newTag)
                tags.add(newTag)
                inputTextField.text = ""
            }
        }
        inputTextField.prefHeightProperty().bind(heightProperty())
        setHgrow(inputTextField, Priority.ALWAYS)
        inputTextField.background = null
        tags.addListener { change: Change<out String> ->
            while (change.next()) {
                if (change.wasPermutated()) {
                    val newSublist = ArrayList<Node?>(change.to - change.from)
                    run {
                        var i = change.from
                        val end = change.to
                        while (i < end) {
                            newSublist.add(null)
                            i++
                        }
                    }
                    var i = change.from
                    val end = change.to
                    while (i < end) {
                        newSublist[change.getPermutation(i)] = children[i]
                        i++
                    }
                    children.subList(change.from, change.to).clear()
                    children.addAll(change.from, newSublist)
                } else {
                    if (change.wasRemoved()) {
                        children.subList(change.from, change.from + change.removedSize).clear()
                    }
                    if (change.wasAdded()) {
                        children.addAll(change.from, change.addedSubList.map { tag: String -> Tag(tag) })
                    }
                }
            }
        }
        children.add(inputTextField)
    }
}