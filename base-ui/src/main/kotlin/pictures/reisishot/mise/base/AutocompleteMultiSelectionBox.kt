package pictures.reisishot.mise.base

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode
import javafx.scene.layout.VBox
import javafx.util.Duration
import org.controlsfx.control.CheckComboBox
import org.controlsfx.control.ListSelectionView
import tornadofx.*


/**
 * Code based on https://stackoverflow.com/a/56644865/1870799 with minor modifications
 */
class AutocompleteMultiSelectionBox<T : Comparable<T>>(private val maxSelectedItems: Int = -1) : VBox() {

    val chooser = ListSelectionView<T>().apply {
        var filter = ""

        onKeyPressed = EventHandler {
            val code = it.code
            when {
                code == KeyCode.BACK_SPACE && filter.isNotEmpty() -> filter = filter.substring(0 until filter.length - 1)
                code == KeyCode.ESCAPE -> filter = ""
                code.isLetterKey -> filter += it.text

            }
            println(filter)
            if (filter.isBlank())
                tooltip?.hide()
            else
                tooltip = tooltip(filter) {
                    hackTooltipStartTiming()
                }

        }

        focusedProperty().addListener { _, _, b ->
            filter = ""
        }
    }

    init {
        children.add(chooser)
        chooser.focusedProperty()
    }
}

fun <T> CheckComboBox<T>.addAll(itemsToAdd: Collection<T>) {
    with(checkModel) {
        clearChecks()
        itemsToAdd.stream()
                .mapToInt { items.indexOf(it) }
                .distinct()
                .forEach { check(it) }

    }
}

// https://stackoverflow.com/a/27739605/1870799
private fun Tooltip.hackTooltipStartTiming() {
    try {
        val fieldBehavior = javaClass.getDeclaredField("BEHAVIOR")
        fieldBehavior.isAccessible = true
        val objBehavior = fieldBehavior.get(this)
        val fieldTimer = objBehavior.javaClass.getDeclaredField("activationTimer")
        fieldTimer.isAccessible = true
        val objTimer = fieldTimer.get(objBehavior) as Timeline
        objTimer.keyFrames.clear()
        objTimer.keyFrames.add(KeyFrame(Duration(100.0)))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}