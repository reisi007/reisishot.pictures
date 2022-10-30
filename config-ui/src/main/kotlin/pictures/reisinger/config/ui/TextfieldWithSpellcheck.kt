package pictures.reisinger.config.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.languagetool.JLanguageTool
import org.languagetool.language.AustrianGerman
import org.languagetool.rules.spelling.SpellingCheckRule

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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextFieldWithSpellcheck(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
    onFocusChanged: (FocusState) -> Unit = {},
    setValue: (String) -> Unit
) = Column {
    rememberCoroutineScope().launch {
        "".performSpellCheck() // slow init....
    }
    val spellcheckErrors by remember(value) { mutableStateOf(value.performSpellCheck()) }
    TextField(
        value,
        setValue,
        modifier = modifier.then(
            Modifier.onKeyEvent {
                when (it.key) {
                    Key.Enter -> {
                        if (onDone != null)
                            onDone()
                        return@onKeyEvent onDone == null
                    }

                    else -> true
                }
            }
                .onFocusChanged { onFocusChanged(it) }
        ),
        singleLine = true,
        isError = spellcheckErrors.isNotEmpty(),
        label = { Text(label) }
    )
    if (spellcheckErrors.isNotEmpty()) {
        Surface(
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(4.dp)
        ) {
            spellcheckErrors.forEach {
                Text(it)
            }
        }
    }
}

fun String.performSpellCheck(): List<String> = spellchecker
    .check(this)
    .map { match ->
        val message = "\"" + substring(match.fromPos, match.toPos) + "\" (" + match.shortMessage + ")"
        if (match.suggestedReplacements.isNotEmpty())
            message + match.suggestedReplacements.joinToString(", ", prefix = ": ") { it }
        else
            message
    }
