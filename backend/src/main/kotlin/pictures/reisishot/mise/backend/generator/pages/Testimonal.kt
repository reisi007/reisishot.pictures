package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.FilenameWithoutExtension
import java.text.SimpleDateFormat
import java.util.*

data class Testimonal(
        val image: FilenameWithoutExtension,
        val name: String,
        val date: String,
        val type: String,
        val text: String
)

private val sourceDateFormat = SimpleDateFormat("yyyy-MM-dd")
private val targetDateFormat = SimpleDateFormat("dd. MMMM yyyy", Locale.GERMAN)


fun Testimonal.dateFormatted() = targetDateFormat.format(sourceDateFormat.parse(date))
