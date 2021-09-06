package pictures.reisishot.mise.configui

import at.reisishot.mise.commons.*
import at.reisishot.mise.config.ImageConfig
import at.reisishot.mise.config.parseConfig
import at.reisishot.mise.config.writeConfig
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
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
import tornadofx.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.streams.asSequence

class MainView : View("Main View") {

    companion object {
        private val regularFilename = """\d{2}_.+?_\d+""".toRegex()
    }

    private val imageView = ImageView().apply {
        isPreserveRatio = true
        maxWidth(Double.POSITIVE_INFINITY)
    }
    private lateinit var lastPath: Path
    private val titleField = TextField().enableSpellcheck {
        errorLabel.text = it.joinToString(System.lineSeparator()) { it }
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

    private val errorLabel = Label().apply {
        textFill = Color.DARKRED
    }

    private val knownTags = mutableSetOf<String>()
    private val imageConfigs = LinkedList<Pair<Path, ImageConfig>>()
    private var initialDirectory: File = File("D:\\Reisishot\\MiSe\\input\\reisishot.pictures\\images")

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

        add(menuBar)
        addInHBox(filenameChooser)
        addInHBox(form)
        addInHBox(errorLabel)
        addInHBox(imageView)

    }

    private fun VBox.addInHBox(child: Node, spacing: Double = 5.0) {
        add(HBox(spacing).apply {
            vgrow = Priority.NEVER
            hgrow = Priority.ALWAYS
            alignment = Pos.CENTER
            with(children) {
                add(child)
            }
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
                            ?.map { it.resolveSibling("${it.fileName.filenameWithoutExtension}.conf") }
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
                                it.parseConfig<ImageConfig>()
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
                    } while (dir == null || !Files.list(dir).anyMatch { it.fileExtension.isConf() })

                    initialDirectory = dir.toFile()
                    val configNoTags = Files.list(dir)
                        .filter { it.fileExtension.isConf() }
                        .map { p -> p to p.parseConfig<ImageConfig>() }
                        .filter { (_, config) -> config != null }
                        .map {
                            @Suppress("UNCHECKED_CAST")
                            it as Pair<Path, ImageConfig>
                        }.filter { (_, config) -> config.tags.isEmpty() }
                        .asSequence()
                    val imagesNoConfig = Files.list(dir)
                        .map { it.resolveSibling(it.filenameWithoutExtension + ".conf") }
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
        if (!lastPath.hasExtension(FileExtension::isConf))
            throw IllegalStateException("Cannot write to file $lastPath, it is not a valid config file!")
        ImageConfig(title, tags = tags).writeConfig(lastPath)
        loadNextImage()
    }

    private fun renameImageIfNeeded() {
        val newFilenameData = filenameChooser.selectedItems.first()
        val oldConfigPath = lastPath
        val oldFilenameData = FilenameData.fromPath(oldConfigPath)
        if (oldFilenameData == newFilenameData)
            return

        val newConfigPath = newFilenameData.getNextFreePath(oldConfigPath)
            .let { it.resolveSibling(it.filenameWithoutExtension + ".conf") }
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
            clear()
            addAll(imageConfig.tags)
        }
        titleField.text = imageConfig.title
        FilenameData.fromPath(path).let {
            filenameChooser.selectedItems = filenameChooser.selectedItems
                .firstOrNull()
                ?.let { sel -> listOf(sel, it).distinct() }
                ?: listOf(it)
        }
    }

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
            .filter { it.fileExtension.isConf() }
            .map { it.parseConfig<ImageConfig>() }
            .filterNotNull()
            .flatMap { it.tags.asSequence() }
            .toList()
    }
}
