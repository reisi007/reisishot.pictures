package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.FilenameWithoutExtension
import java.text.SimpleDateFormat
import java.util.*

data class Testimonal(
        val image: FilenameWithoutExtension,
        val name: String,
        private val _date: String,
        val type: String,
        val html: String
) {
    companion object {
        private val sourceDateFormat = SimpleDateFormat("yyyy-MM-dd")
        private val targetDateFormat = SimpleDateFormat("dd. MMMM yyyy", Locale.GERMAN)
    }

    val date: Date by lazy {
        sourceDateFormat.parse(_date)
    }
    val formattedDate: String by lazy {
        targetDateFormat.format(date)
    }
}
