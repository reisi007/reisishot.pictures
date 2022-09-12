package pictures.reisishot.mise.configui

import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch

class ConfigUi : App(MainView::class) {
    override fun start(stage: Stage) = with(stage) {
        title = "reisinger.pictures config UI"
        minWidth = 900.0
        minHeight = 950.0
        centerOnScreen()
        super.start(this)
    }
}

fun main() {
    launch<ConfigUi>()
}
