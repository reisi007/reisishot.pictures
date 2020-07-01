package pictures.reisishot.mise.base

import javafx.animation.PauseTransition
import javafx.event.EventHandler
import javafx.scene.control.TextInputControl
import javafx.util.Duration
import org.languagetool.JLanguageTool
import org.languagetool.language.AustrianGerman
import tornadofx.*
import java.util.concurrent.atomic.AtomicBoolean

private val spellchecker by lazy { JLanguageTool(AustrianGerman()) }

private val isSpellCheckerInitialized = AtomicBoolean(false)

fun <T : TextInputControl> T.enableSpellcheck(consumer: T.(List<String>) -> Unit = {}) = apply {
    val debouncing = PauseTransition(Duration(500.0))
            .apply {
                onFinished = EventHandler {
                    consumer(performSpellcheck())
                }
            }
    if (!isSpellCheckerInitialized.get()) {
        runAsync {
            finally {
                println("Spell check: " + spellchecker.language)
            }
            spellchecker.check("init")
            isSpellCheckerInitialized.lazySet(true)
        }
    }

    onKeyReleased = EventHandler {
        debouncing.playFromStart()
    }
}

private fun TextInputControl.performSpellcheck(): List<String> = performSpellCheck(text)

private fun performSpellCheck(text: String): List<String> = if (isSpellCheckerInitialized.get())
    spellchecker
            .check(text)
            .map { match ->
                val message = "\"" + text.substring(match.fromPos, match.toPos) + "\" (" + match.shortMessage + ")"
                if (match.suggestedReplacements.isNotEmpty())
                    message + match.suggestedReplacements.joinToString(", ", prefix = ": ") { it }
                else
                    message
            }
else emptyList()

