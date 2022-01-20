package pictures.reisishot.mise.base

import javafx.geometry.Insets

fun insectsOf(
    top: Int = 0,
    right: Int = 0,
    bottom: Int = 0,
    left: Int = 0
): Insets = Insets(top.toDouble(), right.toDouble(), bottom.toDouble(), left.toDouble())
