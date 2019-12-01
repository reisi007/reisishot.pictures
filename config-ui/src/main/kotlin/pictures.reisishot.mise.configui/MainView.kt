package pictures.reisishot.mise.configui

import at.reisishot.mise.commons.fileExtension
import at.reisishot.mise.commons.filenameWithoutExtension
import at.reisishot.mise.commons.isConf
import at.reisishot.mise.commons.isRegularFile
import at.reisishot.mise.config.ImageConfig
import at.reisishot.mise.config.parseConfig
import at.reisishot.mise.config.writeConfig
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.streams.asSequence

class MainView : View("Main View") {
    private val imageView = ImageView().apply {
        isPreserveRatio = true
        maxWidth(Double.POSITIVE_INFINITY)
    }
    private lateinit var lastPath: Path
    private val titleField = TextField()
    private val tagField = AutocompleteMultiSelectionBox()
    private val saveButton = Button("Speichern").apply {
        hgrow = Priority.ALWAYS
        maxWidth = Double.POSITIVE_INFINITY
        setOnAction {
            saveImageConfig()
        }
    }

    private val knownTags = tagField.suggestions
    private val imageConfigs = LinkedList<Pair<Path, ImageConfig>>()
    private var initialDirectory: File = File("D:\\Reisishot\\MiSe\\input\\images")


    override val root = vbox(5) {

        val menuBar = getMenubar()
        val form = getEditFields()

        imageView.fitWidthProperty().bind(widthProperty())
        imageView.fitHeightProperty().bind(heightProperty() - menuBar.heightProperty() - form.heightProperty() - saveButton.heightProperty() - ((children.size) * spacing))

        add(menuBar)
        addInHBox(imageView)
        addInHBox(form)
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
                    var path: Path?
                    var parsedConfig: ImageConfig?
                    do {
                        path = with(FileChooser()) {
                            initialDirectory = this@MainView.initialDirectory
                            extensionFilters.add(FileChooser.ExtensionFilter("Config files", "*.conf"))
                            showOpenDialog(null)
                        }?.toPath()
                        parsedConfig = path?.parseConfig<ImageConfig>()
                    } while (path == null || parsedConfig == null)
                    initialDirectory = path.toFile()
                    loadImageConfig(sequenceOf(path to parsedConfig))
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
                    loadImageConfig(
                            Files.list(dir)
                                    .filter { it.fileExtension.isConf() }
                                    .map { p -> p to p.parseConfig<ImageConfig>() }
                                    .filter { (_, config) -> config != null }
                                    .map {
                                        @Suppress("UNCHECKED_CAST")
                                        it as Pair<Path, ImageConfig>
                                    }.filter { (_, config) -> config.tags.isEmpty() }
                                    .asSequence()
                    )
                }
            }
        }
    }


    private fun loadImageConfig(fileSequence: Sequence<Pair<Path, ImageConfig>>) {
        imageConfigs.addAll(fileSequence.toList())
        imageConfigs.firstOrNull()?.let { (p, _) ->
            knownTags.clear()
            knownTags.addAll(p.loadTagList())
        }
        loadNextImage()
    }

    private fun saveImageConfig() {
        val tags = tagField.tags.toSet()
        val title = titleField.text
        ImageConfig(title, tags = tags).writeConfig(lastPath)
        loadNextImage()
    }

    private fun loadNextImage() {
        val (path, imageConfig) = imageConfigs.firstOrNull() ?: return
        imageConfigs.removeFirst()
        lastPath = path
        imageView.image = Image(Files.newInputStream(path.let { it.resolveSibling(it.filenameWithoutExtension + ".jpg") }))
        tagField.tags.addAll(imageConfig.tags)
        titleField.text = imageConfig.title
    }

    private fun Path.loadTagList(): List<String> {
        if (isRegularFile())
            return parent?.loadTagList() ?: emptyList()
        return Files.list(this)
                .asSequence()
                .filter { it.fileExtension.isConf() }
                .map { it.parseConfig<ImageConfig>() }
                .filterNotNull()
                .flatMap { it.tags.asSequence() }
                .toList()
    }
}

