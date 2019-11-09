package pictures.reisishot.mise.configui

import javafx.beans.property.ReadOnlyDoubleProperty
import tornadofx.*

operator fun Int.times(value: ReadOnlyDoubleProperty) = value * this
operator fun Double.times(value: ReadOnlyDoubleProperty) = value * this
