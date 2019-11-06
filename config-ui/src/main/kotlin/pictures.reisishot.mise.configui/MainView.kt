package pictures.reisishot.mise.configui

import at.reisishot.mise.commons.fileExtension
import at.reisishot.mise.commons.filenameWithoutExtension
import at.reisishot.mise.commons.isConf
import at.reisishot.mise.config.ImageConfig
import at.reisishot.mise.config.parseConfig
import at.reisishot.mise.config.writeConfig
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
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
        minHeight(400.0)
        maxWidth(Double.POSITIVE_INFINITY)
    }
    private val titleField = TextField().apply {

    }
    private val tagField = TextField().apply {

    }

    private val imageConfigs = LinkedList<Pair<Path, ImageConfig>>()
    private var initialDirectory: File? = null


    override val root = vbox(5) {

        menubar {
            menu("Datei") {
                item("Config öffnen") {
                    setOnAction {
                        var path: Path?
                        var parsedConfig: ImageConfig?
                        do {
                            path = with(FileChooser()) {
                                showOpenDialog(null)
                            }?.toPath()
                            parsedConfig = path?.parseConfig<ImageConfig>()
                        } while (path == null || parsedConfig == null)
                        loadImageConfig(sequenceOf(path to parsedConfig))
                    }

                }
                item("Fehlende Configs öffnen") {
                    setOnAction {
                        var dir: Path?
                        do {
                            dir = with(DirectoryChooser()) {
                                initialDirectory = this.initialDirectory
                                val result = showDialog(null)
                                this.initialDirectory = result
                                result
                            }?.toPath()


                        } while (dir == null || !Files.list(dir).anyMatch { it.fileExtension.isConf() })

                        loadImageConfig(
                                Files.list(dir)
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

        val form = Form().apply {
            fieldset("Meta-Informationen") {
                field("Titel") {
                    add(titleField)
                }
                field("Tags") {
                    add(tagField)
                }
            }
        }

        val saveButton = Button("Speichern").apply {
            hgrow = Priority.ALWAYS
            maxWidth = Double.POSITIVE_INFINITY
            setOnAction {
                if (imageConfigs.isNotEmpty())
                    saveImageConfig(imageConfigs.first())
            }

        }

        imageView.isPreserveRatio = true
        imageView.fitWidthProperty().bind(widthProperty())
        imageView.fitHeightProperty().bind(heightProperty() - (form.heightProperty() + saveButton.heightProperty()))

        with(children) {
            add(HBox().apply {
                alignment = Pos.CENTER
                with(children) {
                    add(imageView)
                }
            })
            add(form)
            add(saveButton)
        }
    }

    private fun loadImageConfig(fileSequence: Sequence<Pair<Path, ImageConfig>>) {
        imageConfigs.addAll(fileSequence.toList())
        loadNextImage()
    }

    private fun saveImageConfig(e: Pair<Path, ImageConfig>) = e.let { (path, c) ->
        c.let { config ->
            val tags = tagField.text.split("\\s")
                    .toSet()
                    .let { if (it.isEmpty()) config.tags else it }
            val title = titleField.text ?: config.title
            config.copy(title, tags = tags)
        }.writeConfig(path)
        if (imageConfigs.isNotEmpty())
            imageConfigs.removeAt(0)
        loadNextImage()
    }

    private fun loadNextImage() {
        val (path, imageConfig) = imageConfigs.removeFirst() ?: return
        imageView.image = Image(Files.newInputStream(path.let { it.resolveSibling(it.filenameWithoutExtension + ".jpg") }))
        tagField.text = imageConfig.tags.joinToString(" ")
        titleField.text = imageConfig.title

    }

}

