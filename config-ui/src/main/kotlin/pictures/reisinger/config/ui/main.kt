package pictures.reisinger.config.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pictures.reisinger.config.ui.components.Center
import pictures.reisinger.config.ui.components.MultiSelectWithSpellcheck
import pictures.reisinger.config.ui.components.MyCard
import pictures.reisinger.config.ui.components.TextFieldWithSpellcheck
import pictures.reisinger.config.ui.components.performSpellCheck
import pictures.reisinger.config.ui.model.ConfigUiViewModel
import pictures.reisinger.config.ui.model.FilenameInfo
import pictures.reisinger.config.ui.model.ImmutableImageConfig
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val ROOT_PATH = Paths.get("..", "..", "Github", "gallery-next", "public", "images")

fun main() = application {


    Window(
        onCloseRequest = ::exitApplication,
        title = "Config UI",
        state = rememberWindowState(width = 1080.dp, height = 1080.dp, position = WindowPosition(Alignment.Center))
    ) {
        val scope = rememberCoroutineScope()
        val vm = remember { ConfigUiViewModel() }

        @Composable
        fun <T> Flow<T>.collectAsStateWithLifecycle(initial: T): T {
            return collectAsState(initial, scope.coroutineContext).value
        }

        scope.launch {
            "".performSpellCheck() // slow init....
        }

        MaterialTheme {
            MenuBar {
                Menu("Datei") {
                    Item("Config öffnen...") {
                        ChooseFiles(
                            ROOT_PATH,
                            FileNameExtensionFilter("Image files", "jpg", "jpeg")
                        ) { pathsToAnalyze -> vm.analyzeFiles(scope, pathsToAnalyze) }
                    }

                    Item("Fehlende Configs öffnen....") {
                        vm.analyzeMissingFiles(scope, ROOT_PATH)
                    }
                }
            }

            val curImage = vm.curImage.collectAsStateWithLifecycle(null)
            val allFilenames = vm.allFilenames.collectAsStateWithLifecycle(emptyList())
            val allTags = vm.allTags.collectAsStateWithLifecycle(emptyList())

            curImage?.let {
                DisplayContent(
                    it,
                    allFilenames,
                    allTags,
                    addToAllTags = { vm.addTag(it) },
                    saveDataAndNext = { imageConfig: ImmutableImageConfig, filenameInfo: FilenameInfo ->
                        vm.saveAndNext(imageConfig, filenameInfo)
                    }
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
    addToAllTags: (String) -> Unit,
    saveDataAndNext: (imageConfig: ImmutableImageConfig, filenameInfo: FilenameInfo) -> Unit
) {
    val (curImagePath, storedConfig) = curImageData
    val filename = curImagePath.filenameWithoutExtension

    var currentFilenameSelection by remember(allFilenames, filename) {
        mutableStateOf(computeDefaultFilename(allFilenames, filename))
    }
    var imageConfig by remember(storedConfig) { mutableStateOf(storedConfig) }

    Scaffold(
        modifier = Modifier.padding(12.dp).fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 32.dp),
                shape = AbsoluteRoundedCornerShape(50),
                onClick = {
                    currentFilenameSelection = computeDefaultFilename(allFilenames, filename)
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
                        setItemSelected = { currentFilenameSelection = setOf(it) },
                        setItemsUnselected = { currentFilenameSelection = emptySet() },
                        selectedItems = currentFilenameSelection
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

                Center {
                    Button(
                        enabled = imageConfig.title.isNotBlank() && imageConfig.tags.isNotEmpty() && currentFilenameSelection.size == 1,
                        onClick = { saveDataAndNext(imageConfig, currentFilenameSelection.first()) }
                    ) {
                        Icon(Icons.Default.Save, "Save")
                        Text("Speichern", modifier = Modifier.padding(start = 8.dp))

                    }
                }
            }


            MyCard(modifier = Modifier.weight(1f, true).fillMaxWidth()) {
                val image = remember(curImagePath) {
                    Files.newInputStream(curImagePath).use {
                        loadImageBitmap(it)
                    }
                }
                Image(
                    bitmap = image,
                    contentDescription = curImagePath.filenameWithoutExtension,
                    modifier = Modifier.fillMaxSize()
                )
            }

        }
    }
}

val startsWithDayOfMonth = """^\d{2}_""".toRegex()

private fun computeDefaultFilename(
    allFilenames: List<FilenameInfo>,
    filename: FilenameWithoutExtension
) = (allFilenames.find { filename.startsWith(it.displayName, true) }
    ?.let {
        if (startsWithDayOfMonth.containsMatchIn(it.displayName))
            null
        else
            it
    }
    ?.let { setOf(it) }
    ?: emptySet())

fun ChooseFiles(root: Path, filenameExtensionFilter: FileNameExtensionFilter, action: (path: Set<Path>) -> Unit) {
    with(JFileChooser(root.toFile())) {
        fileFilter = filenameExtensionFilter
        isMultiSelectionEnabled = true
        val returnVal = showOpenDialog(null)
        if (returnVal == JFileChooser.APPROVE_OPTION)
            action(selectedFiles.asSequence().map { it.toPath() }.toSet())
    }
}
