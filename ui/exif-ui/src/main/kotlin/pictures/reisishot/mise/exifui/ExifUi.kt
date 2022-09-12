package pictures.reisishot.mise.exifui

import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch

class ExifUi : App(MainView::class) {
    override fun start(stage: Stage) = with(stage) {
        title = "reisinger.pictures config UI"
        minWidth = 800.0
        minHeight = 800.0
        centerOnScreen()
        super.start(this)
    }
}

fun main() {
    launch<ExifUi>()
}
