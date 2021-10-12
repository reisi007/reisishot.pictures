package pictures.reisishot.mise.base

import javafx.animation.KeyFrame
import javafx.animation.PauseTransition
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.control.TextInputControl
import javafx.scene.control.Tooltip
import javafx.util.Duration
import org.languagetool.JLanguageTool
import org.languagetool.language.AustrianGerman
import org.languagetool.rules.spelling.SpellingCheckRule
import tornadofx.*
import java.util.concurrent.atomic.AtomicBoolean

private val spellchecker by lazy {
    val spellchecker = JLanguageTool(AustrianGerman())

    val ignoredWords by lazy {
        val ignoredWords = ClassLoader.getSystemClassLoader()
            .getResourceAsStream("ignore.txt")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.lines().toList() }
            ?: emptyList()

        println("${ignoredWords.size} Wörter werden ignoriert")
        ignoredWords
    }

    spellchecker.allRules
        .asSequence()
        .map { it as? SpellingCheckRule }
        .filterNotNull()
        .forEach {
            it.addIgnoreTokens(ignoredWords)

        }

    spellchecker
}

private val isSpellCheckerInitialized = AtomicBoolean(false)

fun <T : TextInputControl> T.enableSpellcheck(consumer: T.(List<String>) -> Unit = { spellcheckTooltipConsumer(it) }) =
    also { t ->
        val debouncing = PauseTransition(Duration(500.0))
            .apply {
                onFinished = EventHandler {
                    consumer(t.performSpellcheck())

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

private fun <T : TextInputControl> T.spellcheckTooltipConsumer(spellingErrors: List<String>) {
    if (spellingErrors.isEmpty())
        tooltip = null
    else
        tooltip {
            text = spellingErrors.joinToString(System.lineSeparator()) { it }
            println(text)
            hackTooltipStartTiming()
            show()
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
