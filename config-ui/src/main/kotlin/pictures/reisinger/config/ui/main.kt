package pictures.reisinger.config.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.runBlocking
import pictures.reisishot.mise.commons.filenameWithoutExtension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Config UI",
        state = rememberWindowState(width = 1280.dp, height = 1080.dp, position = WindowPosition(Alignment.Center))
    ) {
        var allFilenames by remember { mutableStateOf(emptyList<FilenameInfo>()) }
        var filesToAnalyze by remember { mutableStateOf(emptyList<Pair<Path, Set<String>>>()) }
        val curImage by remember(filesToAnalyze) { mutableStateOf(filesToAnalyze.getOrNull(0)) }

        MaterialTheme {
            MenuBar {
                Menu("Datei") {
                    /* Item("Config öffnen...") {

             }*/

                    Item("Fehlende Configs öffnen....") {
                        val pathToAnalyze = Paths.get(".", "input", "reisinger.pictures", "images")
                        runBlocking {
                            launch { filesToAnalyze = pathToAnalyze.findMissingFiles() }
                            launch { allFilenames = pathToAnalyze.findUsedFilenames() }
                        }
                    }
                }
            }
            curImage?.let { DisplayContent(it, allFilenames) } ?: kotlin.run {
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
    curImageData: Pair<Path, Set<String>>,
    allFilenames: List<FilenameInfo>
) {
    val (curImage, storedTags) = curImageData
    Column(modifier = Modifier.padding(8.dp).fillMaxSize()) {
        var selectedItems by remember(curImage) { mutableStateOf(emptySet<FilenameInfo>()) }
        Center {
            MyCard {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("Name: ")
                    MultiSelect(allFilenames, selectedItems) {
                        selectedItems = setOf(it)
                    }
                }
            }
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


