package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.backend.df_dd_MMMM_yyyy
import pictures.reisishot.mise.backend.df_yyyy_MM_dd
import java.util.*

data class Testimonal(
        val image: FilenameWithoutExtension,
        val name: String,
        val isoDateString: String,
        val type: String,
        val html: String
) {
    val date: Date by lazy {
        df_yyyy_MM_dd.parse(isoDateString)
    }
    val formattedDate: String by lazy {
        df_dd_MMMM_yyyy.format(date)
    }
}
