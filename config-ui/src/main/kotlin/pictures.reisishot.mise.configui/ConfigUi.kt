package pictures.reisishot.mise.configui

import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch

class ConfigUi : App(MainView::class) {
    override fun start(stage: Stage) = with(stage) {
        minWidth = 800.0
        minHeight = 600.0
        centerOnScreen()
        super.start(this)
    }
}

fun main() {
    launch<ConfigUi>()
}