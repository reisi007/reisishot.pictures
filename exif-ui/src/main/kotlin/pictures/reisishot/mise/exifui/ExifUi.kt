package pictures.reisishot.mise.exifui

import javafx.stage.Stage
import tornadofx.*

class ExifUi : App(MainView::class) {
    override fun start(stage: Stage) = with(stage) {
        title = "reisishot.pictures config UI"
        minWidth = 800.0
        minHeight = 800.0
        centerOnScreen()
        super.start(this)
    }
}

fun main() {
    launch<ExifUi>()
}