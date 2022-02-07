package pictures.reisishot.mise.backend.generator.testimonials

import pictures.reisishot.mise.backend.df_dd_MMMM_yyyy
import pictures.reisishot.mise.backend.df_yyyy_MM_dd
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import java.util.*

class Testimonial(
    val id: String,
    val image: FilenameWithoutExtension?,
    val images: List<FilenameWithoutExtension>?,
    val video: String?,
    val rating: Int?, // between 0 and 100
    val name: String,
    val isoDateString: String,
    val type: String,
    html: String
) : Comparable<Testimonial> {
    val date: Date by lazy {
        df_yyyy_MM_dd.parse(isoDateString)
    }
    val formattedDate: String by lazy {
        df_dd_MMMM_yyyy.format(date)
    }

    val html = html.ifBlank { null }


    override fun compareTo(other: Testimonial): Int {
        return COMPARATOR.compare(this, other)
    }

    override fun toString(): String {
        return "Testimonial(id='$id', image=$image, images=$images, video=$video, rating=$rating, name='$name', isoDateString='$isoDateString', type='$type', html=$html)"
    }

    companion object {
        val COMPARATOR = compareBy<Testimonial>(
            { it.image == null && it.video == null && it.images == null },
            { it.html == null },
        ).thenDescending(compareBy { it.isoDateString })
            .thenDescending(compareBy { it.rating ?: -1 })
            .thenDescending(compareBy { it.html?.length ?: -1 })
    }


}
