package pictures.reisishot.mise.configui

import javafx.animation.KeyFrame
import javafx.animation.PauseTransition
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.control.TextInputControl
import javafx.scene.control.Tooltip
import javafx.util.Duration
import org.languagetool.JLanguageTool
import org.languagetool.language.AustrianGerman
import tornadofx.*

private val spellcheckerDelegate = lazy { JLanguageTool(AustrianGerman()) }
private val spellchecker by spellcheckerDelegate


private val debouncing = mutableMapOf<TextInputControl, PauseTransition>()

fun <T : TextInputControl> T.enableSpellcheck() = apply {
    onKeyReleased = EventHandler {
        performSpellcheckEventListener()
    }
}

private fun <T : TextInputControl> T.performSpellcheckEventListener() {
    if (!spellcheckerDelegate.isInitialized()) {
        println("Spell check: " + spellchecker.language)
        spellchecker.check("")
    }

    val debouncing = debouncing.computeIfAbsent(this) {
        PauseTransition(Duration(200.0)).apply {
            onFinished = EventHandler {
                val spellingErrors = performSpellcheck()
                if (spellingErrors.isEmpty())
                    this@performSpellcheckEventListener.tooltip = null
                else
                    tooltip {
                        text = spellingErrors.joinToString(System.lineSeparator()) { it }
                        println(text)
                        hackTooltipStartTiming()
                        show()
                    }
            }

        }
    }
    debouncing.playFromStart()
}

private fun TextInputControl.performSpellcheck(): List<String> = text.let { text ->
    spellchecker
            .check(text)
            .map { match ->
                val message = "\"" + text.substring(match.fromPos, match.toPos) + "\" (" + match.shortMessage + ")"
                if (match.suggestedReplacements.isNotEmpty())
                    message + match.suggestedReplacements.joinToString(", ", prefix = ": ") { it }
                else
                    message
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