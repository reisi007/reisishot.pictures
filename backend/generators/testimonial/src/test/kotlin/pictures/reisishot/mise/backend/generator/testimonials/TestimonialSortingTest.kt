package pictures.reisishot.mise.backend.generator.testimonials

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.commons.toTypedArray

class TestimonialSortingTest {

    @Test
    fun `sorting of testimonials work`() {
        var id = 1
        val testimonials = sequenceOf(
            createTestimonial(id++, DATE_NEWEST, "img", html = "Review"),
            createTestimonial(id++, DATE_MIDDLE, "img", 5, "Review"),
            createTestimonial(id++, DATE_OLDEST, "img", 4, "Longer Review"),
            createTestimonial(id++, DATE_NEWEST, html = "Longer Review"),
            createTestimonial(id++, DATE_NEWEST, html = "Review"),
            createTestimonial(id++, DATE_MIDDLE, rating = 5, html = "Review"),
            createTestimonial(id++, DATE_OLDEST, rating = 4, html = "Longer Review"),
            createTestimonial(id, DATE_NEWEST)
        )

        val ordered = testimonials.shuffled()
            .sorted()
            .toList()
        val actual = ordered
            .map { it.id }

        val order = (1..id).asSequence().map { it.toString() }.toTypedArray()

        Assertions.assertThat(actual)
            .`as` { ordered.joinToString(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()) }
            .containsExactly(*order)
    }

    companion object {
        private const val DATE_NEWEST = "2022-01-01"
        private const val DATE_MIDDLE = "2021-07-01"
        private const val DATE_OLDEST = "2021-01-01"
    }
}
