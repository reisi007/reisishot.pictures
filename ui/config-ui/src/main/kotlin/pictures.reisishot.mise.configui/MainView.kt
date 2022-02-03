package pictures.reisishot.mise.configui

import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import pictures.reisishot.mise.base.AutocompleteMultiSelectionBox
import pictures.reisishot.mise.base.enableSpellcheck
import pictures.reisishot.mise.base.insectsOf
import pictures.reisishot.mise.commons.*
import pictures.reisishot.mise.config.ImageConfig
import pictures.reisishot.mise.ui.json.fromJson
import pictures.reisishot.mise.ui.json.toJson
import tornadofx.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.exists
import kotlin.streams.asSequence

class MainView : View("Main View") {

    companion object {
        private val regularFilename = """\d{2}_.+?_\d+(-Edit)*""".toRegex()
    }

    private val imageView = ImageView().apply {
        isPreserveRatio = true
        maxWidth(Double.POSITIVE_INFINITY)
    }
    private lateinit var lastPath: Path
    private val titleField = TextField().enableSpellcheck { errors ->
        errorLabel.text = errors.joinToString(System.lineSeparator()) { it }
    }
    private val tagField = AutocompleteMultiSelectionBox { it }
    private val filenameChooser = FilenameChooser()
    private val saveButton = Button("Speichern").apply {
        hgrow = Priority.ALWAYS
        maxWidth = Double.POSITIVE_INFINITY
        setOnAction {
            saveImageConfig()
        }
    }

    private val reset = CheckBox("Reset nach Bild").apply {
        isSelected = false

    }

    private val errorLabel = Label().apply {
        textFill = Color.DARKRED
    }

    private val knownTags = mutableSetOf<String>()
    private val imageConfigs = LinkedList<Pair<Path, ImageConfig>>()
    private var initialDirectory: File =
        Paths.get(".", "input\\reisishot.pictures\\images")
            .toAbsolutePath()
            .normalize()
            .toFile()

    override val root = vbox(5) {

        val menuBar = getMenubar()
        val form = getEditFields()

        imageView.fitWidthProperty().bind(widthProperty())
        imageView.fitHeightProperty().bind(
            heightProperty() -
                    filenameChooser.heightProperty() -
                    menuBar.heightProperty() -
                    form.heightProperty() -
                    saveButton.heightProperty() -
                    errorLabel.heightProperty() -
                    ((children.size) * spacing)
        )

        val resetButton = button("Reset") {
            setOnAction {
                if (filenameChooser.selectedItems.isNotEmpty())
                    filenameChooser.selectedItems = listOf(filenameChooser.selectedItems.last())
                titleField.clear()
                tagField.suggestions.clear()
                tagField.suggestions += knownTags
                tagField.tags.clear()
            }
        }

        add(menuBar)
        addInHBox(filenameChooser, resetButton, reset) {
            HBox.setMargin(reset, insectsOf(right = 30))

        }
        addInHBox(form)
        addInHBox(errorLabel)
        addInHBox(imageView)

    }


    private fun VBox.addInHBox(vararg child: Node, spacing: Double = 5.0, configurator: HBox.() -> Unit = {}) {
        add(HBox(spacing).apply {
            vgrow = Priority.NEVER
            hgrow = Priority.ALWAYS
            alignment = Pos.CENTER
            with(children) {
                addAll(child)
            }
            configurator(this)
        })
    }

    private fun getEditFields() = Form().apply {
        vgrow = Priority.NEVER
        hgrow = Priority.ALWAYS
        fieldset("Meta-Informationen") {
            field("Titel") {
                add(titleField)
            }
            field("Tags") {
                add(tagField)
            }
        }

        add(saveButton)
    }


    private fun EventTarget.getMenubar() = menubar {
        menu("Datei") {
            item("Config öffnen") {
                setOnAction {
                    var path: List<Path>

                    with(FileChooser()) {
                        initialDirectory = this@MainView.initialDirectory
                        extensionFilters.add(FileChooser.ExtensionFilter("Image files", "*.jpg", "*.jpeg"))
                        val showOpenMultipleDialog = showOpenMultipleDialog(null)
                        path = showOpenMultipleDialog
                            ?.asSequence()
                            ?.map { it.toPath() }
                            ?.map { it.resolveSibling("${it.fileName.filenameWithoutExtension}.json") }
                            ?.toList() ?: emptyList()
                    }
                    if (path.isEmpty())
                        return@setOnAction
                    path.first().parent?.toFile()?.let {
                        initialDirectory = it
                    }
                    val configs = path.asSequence()
                        .map {
                            it to if (it.exists())
                                it.fromJson<ImageConfig>()
                                    ?: throw IllegalStateException("Cannot load image config from file \"$it\"!")
                            else
                                ImageConfig("", tags = mutableSetOf())
                        }

                    loadImageConfig(configs)
                }

            }
            item("Fehlende Configs öffnen") {
                setOnAction {
                    var dir: Path?
                    do {
                        dir = with(DirectoryChooser()) {
                            initialDirectory = this@MainView.initialDirectory
                            val result: File? = showDialog(null)
                            this.initialDirectory = result
                            result
                        }?.toPath()
                    } while (dir == null || !Files.list(dir).anyMatch { it.fileExtension.isJson() })

                    initialDirectory = dir.toFile()
                    val configNoTags = Files.list(dir)
                        .filter { it.fileExtension.isJson() }
                        .map { p -> p to p.fromJson<ImageConfig>() }
                        .filter { (_, config) -> config != null }
                        .map {
                            @Suppress("UNCHECKED_CAST")
                            it as Pair<Path, ImageConfig>
                        }.filter { (_, config) -> config.tags.isEmpty() }
                        .asSequence()
                    val imagesNoConfig = Files.list(dir)
                        .map { it.resolveSibling(it.filenameWithoutExtension + ".json") }
                        .filter { !Files.exists(it) }
                        .map { it to ImageConfig("", tags = mutableSetOf()) }
                        .asSequence()
                    loadImageConfig(
                        imagesNoConfig + configNoTags
                    )
                }
            }
        }
    }


    private fun loadImageConfig(fileSequence: Sequence<Pair<Path, ImageConfig>>) {
        val files = fileSequence.toList()
        imageConfigs.addAll(files)

        imageConfigs.firstOrNull()?.let { (first, _) ->
            knownTags.clear()
            knownTags.addAll(first.loadTagList())
            first.loadFilenameData()
            filenameChooser.accept(first)
        }
        loadNextImage()
    }

    private fun saveImageConfig() {
        val tags = tagField.tags.toMutableSet()
        val title = titleField.text
        knownTags += tags
        renameImageIfNeeded()
        if (!lastPath.hasExtension(FileExtension::isJson))
            throw IllegalStateException("Cannot write to file $lastPath, it is not a valid config file!")
        ImageConfig(title, tags = tags).toJson(lastPath)
        loadNextImage()
    }

    private fun renameImageIfNeeded() {
        val newFilenameData = filenameChooser.selectedItems.first()
        val oldConfigPath = lastPath
        val oldFilenameData = FilenameData.fromPath(oldConfigPath)
        if (oldFilenameData == newFilenameData)
            return

        val newConfigPath = newFilenameData.getNextFreePath(oldConfigPath)
            .let { it.resolveSibling(it.filenameWithoutExtension + ".json") }
        val oldImagePath = oldConfigPath.resolveSibling(oldConfigPath.filenameWithoutExtension + ".jpg")
        val newImagePath = newConfigPath.resolveSibling(newConfigPath.filenameWithoutExtension + ".jpg")

        if (Files.exists(oldConfigPath))
            Files.move(oldConfigPath, newConfigPath, StandardCopyOption.ATOMIC_MOVE)
        if (Files.exists(oldImagePath))
            Files.move(oldImagePath, newImagePath, StandardCopyOption.ATOMIC_MOVE)

        lastPath = newConfigPath
    }

    private fun loadNextImage() {
        val (path, imageConfig) = imageConfigs.firstOrNull() ?: return
        imageConfigs.removeFirst()
        lastPath = path
        imageView.image =
            Image(Files.newInputStream(path.let { it.resolveSibling(it.filenameWithoutExtension + ".jpg") }))
        with(tagField.suggestions) {
            clear()
            addAll(knownTags)
        }
        with(tagField.tags) {
            if (resetValuesOnImageChange()) {
                titleField.text = imageConfig.title
                clear()
            }
            addAll(imageConfig.tags)
        }
        if (imageConfig.title.isNotBlank()) {
            titleField.text = imageConfig.title
        }

        FilenameData.fromPath(path).let { computedFilename ->
            filenameChooser.selectedItems = filenameChooser.selectedItems
                .firstOrNull()
                ?.let { if (resetValuesOnImageChange()) null else it }
                ?.let { sel -> listOf(sel, computedFilename).distinct() }
                ?: listOf(computedFilename)
        }
    }

    private fun resetValuesOnImageChange() = reset.isSelected

    private fun Path.loadFilenameData() {
        if (!Files.isDirectory(this))
            parent?.loadFilenameData()
        else {
            filenameChooser.items.clear()
            Files.list(this)
                .filter { it.isRegularFile() }
                .filter { it.fileExtension.isJpeg() }
                .filter { !regularFilename.matches(it.filenameWithoutExtension) }
                .forEach { filenameChooser.accept(it) }

            println()
            filenameChooser.items
                .asSequence()
                .sortedBy { it.name }
                .forEach { println(it) }
            println()
        }
    }

    private fun Path.loadTagList(): List<String> {
        if (!Files.isDirectory(this))
            return parent?.loadTagList() ?: emptyList()
        return Files.list(this)
            .asSequence()
            .filter { Files.exists(it) }
            .filter { it.fileExtension.isJson() }
            .map { it.fromJson<ImageConfig>() }
            .filterNotNull()
            .flatMap { it.tags.asSequence() }
            .toList()
    }
}
