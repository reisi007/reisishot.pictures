package pictures.reisishot.mise.backend.generator.testimonials

import at.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.backend.df_dd_MMMM_yyyy
import pictures.reisishot.mise.backend.df_yyyy_MM_dd
import java.util.*

data class Testimonial(
    val id: String,
    val image: FilenameWithoutExtension?,
    val images: List<FilenameWithoutExtension>?,
    val video: String?,
    val rating: Int?, // between 0 and 100
    val name: String,
    val isoDateString: String,
    val type: String,
    val html: String?
) {
    val date: Date by lazy {
        df_yyyy_MM_dd.parse(isoDateString)
    }
    val formattedDate: String by lazy {
        df_dd_MMMM_yyyy.format(date)
    }
}
