package pictures.reisinger.config.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.languagetool.JLanguageTool
import org.languagetool.language.AustrianGerman
import org.languagetool.rules.spelling.SpellingCheckRule
import kotlin.time.Duration.Companion.milliseconds

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

@OptIn(ExperimentalComposeUiApi::class, FlowPreview::class)
@Composable
fun TextFieldWithSpellcheck(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onFocusChanged: (FocusState) -> Unit = {},
    setValue: (String) -> Unit
) = Column {

    val scope = rememberCoroutineScope()
    val coroutineContext = scope.coroutineContext
    val mutableValue = remember { MutableStateFlow(value) }
    val spellcheckErrors = remember(mutableValue) {
        mutableValue
            .debounce(200.milliseconds)
            .map { it.performSpellCheck() }
    }
        .collectAsState(emptyList(), coroutineContext)
        .value

    TextField(
        value,
        onValueChange = {
            scope.launch {
                setValue(it)
                mutableValue.emit(it)
            }
        },
        modifier = modifier.then(
            Modifier.onKeyEvent {
                when (it.key) {
                    Key.Enter -> {
                        if (onDone != null)
                            onDone()
                        return@onKeyEvent onDone == null
                    }

                    Key.DirectionDown -> {
                        if (onDown != null)
                            onDown()
                        return@onKeyEvent onDown == null
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
    if (spellcheckErrors.isNotEmpty() && value.isNotBlank()) {
        Surface(
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(4.dp)
        ) {
            Column {
                spellcheckErrors.forEach {
                    Text(it)
                }
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
