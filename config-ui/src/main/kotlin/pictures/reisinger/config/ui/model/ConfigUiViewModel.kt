package pictures.reisinger.config.ui.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pictures.reisishot.mise.commons.withNewExtension
import pictures.reisishot.mise.json.toJson
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.moveTo

class ConfigUiViewModel {

    private val mutableAllFilenames = MutableStateFlow(emptyList<FilenameInfo>())
    private val mutableAllTags = MutableStateFlow(emptyList<String>())
    private val filesToAnalyze = MutableStateFlow(emptyList<Pair<Path, ImmutableImageConfig>>())
    internal val curImage = filesToAnalyze.map { it.firstOrNull() }

    internal val allFilenames: StateFlow<List<FilenameInfo>>
        get() = mutableAllFilenames

    internal val allTags: StateFlow<List<String>>
        get() = mutableAllTags

    fun analyzeFiles(scope: CoroutineScope, pathsToAnalyze: Set<Path>) {
        val commonParent: Path = pathsToAnalyze.first().parent

        with(scope) {
            launch { filesToAnalyze.emit(pathsToAnalyze.readExistingConfigFiles()) }
            launch { mutableAllFilenames.emit(commonParent.findUsedFilenames()) }
            launch { mutableAllTags.emit(commonParent.findAllTags()) }
        }
    }

    fun analyzeMissingFiles(scope: CoroutineScope, path: Path): Unit = with(scope) {
        launch { filesToAnalyze.emit(path.findMissingFiles()) }
        launch { mutableAllFilenames.emit(path.findUsedFilenames()) }
        launch { mutableAllTags.emit(path.findAllTags()) }
    }


    fun saveAndNext(imageConfig: ImmutableImageConfig, newFilename: FilenameInfo) {
        val (curImagePath) = filesToAnalyze.value.first()
        val isPathValid = curImagePath.toFileInfo()?.let { it.displayName == newFilename.displayName } ?: false
        val pathToStoreJson = if (isPathValid) {
            curImagePath.withNewExtension("json")
        } else {
            val newJsonPath = newFilename.nextFreeFilename(curImagePath.parent, "json")
            val newImageLocation = newJsonPath.withNewExtension("jpg")
            curImagePath.withNewExtension("json").deleteIfExists()

            curImagePath.moveTo(newImageLocation)

            newJsonPath
        }

        imageConfig.toJson(pathToStoreJson)

        filesToAnalyze.update {
            it.subList(1, it.size)
        }
    }

    fun addTag(newTag: String) = mutableAllTags.update {
        it.toMutableSet()
            .apply { add(newTag) }
            .sorted()
    }
}
