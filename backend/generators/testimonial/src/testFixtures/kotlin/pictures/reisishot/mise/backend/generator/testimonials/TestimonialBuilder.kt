package pictures.reisishot.mise.backend.generator.testimonials

import pictures.reisishot.mise.commons.FilenameWithoutExtension
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories

fun Path.createTestimonial(
    id: String,
    image: FilenameWithoutExtension? = id,
    images: List<FilenameWithoutExtension>? = null,
    video: String? = null,
    rating: Int = 100,
    name: String = "Martha Musterfrau",
    isoDateString: String = "2021-02-01",
    type: String = "test",
    md: String = """
        Das Shooting ist super
    """.trimIndent()
) {
    createDirectories()
    resolve("${id.lowercase()}.review.md").bufferedWriter(options = arrayOf(CREATE, TRUNCATE_EXISTING)).use {

        val count = sequenceOf(image, images, video)
            .filterNotNull()
            .count()
        if (count != 1)
            error("Only one media is allowed")

        with(it) {
            appendLine("---")
            appendIfValueNotNull("image", image)
            appendIfValueNotNull("images", images)
            appendIfValueNotNull("video", video)
            appendIfValueNotNull("rating", rating.toString())
            appendIfValueNotNull("name", name)
            appendIfValueNotNull("date", isoDateString)
            appendIfValueNotNull("type", type)
            appendLine("---")
            appendLine(md)
        }
    }
}

private fun Appendable.appendIfValueNotNull(key: String, value: String?) {
    if (value == null) return
    appendLine("$key: $value")
}

private fun Appendable.appendIfValueNotNull(key: String, value: List<String>?) {
    if (value == null) return
    appendLine("$key:")
    value.forEach {
        appendLine(" - $it")
    }
}

fun createTestimonial(
    id: Int,
    isoDateString: String,
    image: FilenameWithoutExtension? = null,
    rating: Int? = null,
    html: String = ""
) = Testimonial(
    id.toString(),
    image,
    null,
    null,
    rating,
    "Test User",
    isoDateString,
    "test",
    html
)

