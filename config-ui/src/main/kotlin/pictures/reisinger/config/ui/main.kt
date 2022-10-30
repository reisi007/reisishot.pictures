package pictures.reisinger.config.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import pictures.reisishot.mise.commons.filenameWithoutExtension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Config UI",
        state = rememberWindowState(width = 1080.dp, height = 1080.dp, position = WindowPosition(Alignment.Center))
    ) {
        var allFilenames by remember { mutableStateOf(emptyList<FilenameInfo>()) }
        var allTags by remember { mutableStateOf(emptyList<String>()) }
        var filesToAnalyze by remember { mutableStateOf(emptyList<Pair<Path, ImmutableImageConfig>>()) }
        val curImage by remember(filesToAnalyze) { mutableStateOf(filesToAnalyze.getOrNull(0)) }
        val scope = rememberCoroutineScope()

        scope.launch {
            "".performSpellCheck() // slow init....
        }

        MaterialTheme {
            MenuBar {
                Menu("Datei") {
                    /* Item("Config öffnen...") {

             }*/

                    Item("Fehlende Configs öffnen....") {
                        val pathToAnalyze = Paths.get(".", "input", "reisinger.pictures", "images")

                        with(scope) {
                            launch { filesToAnalyze = pathToAnalyze.findMissingFiles() }
                            launch { allFilenames = pathToAnalyze.findUsedFilenames() }
                            launch { allTags = pathToAnalyze.findAllTags() }
                        }
                    }
                }
            }

            curImage?.let {
                DisplayContent(
                    it,
                    allFilenames,
                    allTags,
                    addToAllTags = { allTags.toMutableSet().apply { add(it) }.sorted() }
                )
            } ?: kotlin.run {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Bitte Configs laden...", style = MaterialTheme.typography.h1)
                }
            }

        }
    }
}

@Composable
private fun DisplayContent(
    curImageData: Pair<Path, ImmutableImageConfig>,
    allFilenames: List<FilenameInfo>,
    allTags: List<String>,
    addToAllTags: (String) -> Unit
) {
    val (curImage, storedConfig) = curImageData

    var selectedItems by remember(curImage) { mutableStateOf(emptySet<FilenameInfo>()) }
    var imageConfig by remember(storedConfig) { mutableStateOf(storedConfig) }

    Scaffold(
        modifier = Modifier.padding(12.dp).fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                shape = AbsoluteRoundedCornerShape(50),
                onClick = {
                    selectedItems = emptySet()
                    imageConfig = ImmutableImageConfig()

                }) {
                Icon(Icons.Default.Restore, "Reset all fields")
            }

        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            MyCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MultiSelectWithSpellcheck(
                        label = "Name",
                        modifier = Modifier.fillMaxWidth(),
                        items = allFilenames,
                        createItem = { FilenameInfo(it, 3) },
                        setItemSelected = { selectedItems = setOf(it) },
                        setItemsUnselected = { selectedItems = emptySet() },
                        selectedItems = selectedItems
                    )
                }

                TextFieldWithSpellcheck(
                    label = "Titel",
                    value = imageConfig.title,
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                ) {
                    imageConfig = imageConfig.copy(title = it)
                }

                MultiSelectWithSpellcheck(
                    label = "Label",
                    items = allTags,
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                    setItemSelected = {
                        addToAllTags(it)
                        imageConfig = imageConfig.copy(tags = imageConfig.tags + it)
                    },
                    setItemsUnselected = { imageConfig = imageConfig.copy(tags = imageConfig.tags - it) },
                    selectedItems = imageConfig.tags,
                    createItem = { it }
                )
            }


            MyCard(modifier = Modifier.weight(1f, true).fillMaxWidth()) {
                val image = remember(curImage) {
                    Files.newInputStream(curImage).use {
                        loadImageBitmap(it)
                    }
                }
                Image(
                    bitmap = image,
                    contentDescription = curImage.filenameWithoutExtension,
                    modifier = Modifier.fillMaxSize()
                )
            }

        }
    }
}


