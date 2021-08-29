package pictures.reisishot.mise.backend.html

import kotlinx.html.FlowContent
import kotlinx.html.HtmlTagMarker
import kotlinx.html.I
import kotlinx.html.i

enum class ReisishotIcons(internal val cssName: String) {
    FB_MESSENGER("messenger"),
    FB("facebook"),
    INSTAGRAM("instagram"),
    WHATSAPP("whatsapp"),
    LINK("link-ext"),
    MAIL("mail"),
    PODCAST("podcast")
}

@HtmlTagMarker
fun FlowContent.insertIcon(
    icon: ReisishotIcons,
    size: String = "lg",
    classes: String = "",
    moreActions: (I) -> Unit = {}
) =
    insertIcon(icon.cssName, size, classes, moreActions)

@HtmlTagMarker
fun FlowContent.insertIcon(iconName: String, size: String = "lg", classes: String = "", moreActions: (I) -> Unit = {}) =
    i("icon rs-$iconName rs-$size $classes", moreActions)
