package pictures.reisishot.mise.backend.config.category.computable

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.category.CategoryComputable
import pictures.reisishot.mise.config.TestGermanLocaleProvider
import pictures.reisishot.mise.config.buildImageInformation
import pictures.reisishot.mise.exifdata.ExifdataKey
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime

class DateCategoryComputableTest {

    @Test
    fun `Parse date from image`() {
        val image = createDateExifInformation()

        val prefix = "test"
        val computable = DateCategoryComputable(prefix)

        computable.matchImage(image, TestGermanLocaleProvider)

        val yearCategories = computable.subcategories
        assertThat(yearCategories.displayNames).containsAll("2020")

        val monthCategories = yearCategories.first().subcategories
        assertThat(monthCategories.displayNames).containsAll("Januar 2020")

        val dayCategories = monthCategories.first().subcategories
        assertThat(dayCategories.displayNames).containsAll("02. Januar 2020")

        assertThat(dayCategories.first().categoryName.complexName).isEqualTo("$prefix/2020/01/02")
    }

    @Test
    fun `Name must be trimmed`() {
        assertThat(DateCategoryComputable(" Test ").complexName).isEqualTo("test")
    }

    @Test
    fun `Base name must be trimmed`() {
        assertThat(DateCategoryComputable("Name", " base ").complexName).isEqualTo("base/name")
    }

    @Test
    fun `Name must not be blank`() {
        val ex = assertThrows<IllegalArgumentException> {
            DateCategoryComputable("   ")
        }

        assertThat(ex).messageContains("blank")
    }

    private val MutableSet<CategoryComputable>.displayNames
        get() = map { it.categoryName.displayName }

    private fun createDateExifInformation(
        year: Int = 2020,
        month: Month = Month.JANUARY,
        dayOfMonth: Int = 2,
        hour: Int = 3,
        minute: Int = 4,
        second: Int = 5
    ): ImageInformation {
        val exif = mapOf(
            ExifdataKey.CREATION_DATETIME to
                    ZonedDateTime.of(
                        LocalDateTime.of(year, month, dayOfMonth, hour, minute, second),
                        ZoneId.systemDefault()
                    ).toString()
        )

        return buildImageInformation(exifInformation = exif)
    }
}
