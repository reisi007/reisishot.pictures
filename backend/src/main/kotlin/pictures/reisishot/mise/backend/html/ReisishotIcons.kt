package pictures.reisishot.mise.backend.html

import kotlinx.html.FlowContent
import kotlinx.html.HtmlTagMarker
import kotlinx.html.i

enum class ReisishotIcons(internal val cssName: String) {
    FB_MESSENGER("messenger"),
    FB("facebook"),
    INSTAGRAM("instagram"),
    WHATSAPP("whatsapp"),
    SKYPE("skype"),
    MAIL("mail"),

}

@HtmlTagMarker
fun FlowContent.insertIcon(icon: ReisishotIcons, size: String = "lg") = insertIcon(icon.cssName, size)

@HtmlTagMarker
fun FlowContent.insertIcon(iconName: String, size: String = "lg") {
    i("icon rs-$iconName rs-$size")
}