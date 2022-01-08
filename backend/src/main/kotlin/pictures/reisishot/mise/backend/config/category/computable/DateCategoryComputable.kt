package pictures.reisishot.mise.backend.config

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.concurrentSetOf
import at.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.config.category.CategoryInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DateCategoryComputable(private val name: String, private val baseName: String? = null) : CategoryComputable {
    private val complexName: String
        get() = (if (baseName == null) "" else "$baseName/") + name
    private val yearSubcategoryMap = ConcurrentHashMap<Int, YearMatcher>()
    override val subcategories: MutableSet<CategoryComputable>
        get() = yearSubcategoryMap.values.toMutableSet()

    override val images: MutableSet<InternalImageInformation> = concurrentSetOf()
    override val categoryName by lazy { CategoryName(complexName) }

    override fun matchImage(imageToProcess: InternalImageInformation, websiteConfiguration: WebsiteConfiguration) {
        val captureDate = imageToProcess.exifInformation[ExifdataKey.CREATION_DATETIME]
            ?.let { ZonedDateTime.parse(it) } ?: return

        val yearMatcher = yearSubcategoryMap
            .computeIfAbsent(captureDate.year) { YearMatcher(complexName, captureDate.year) }

        val monthMatcher = yearMatcher
            .monthSubcategoryMap
            .computeIfAbsent(captureDate.month) {
                MonthMatcher(
                    yearMatcher.complexName,
                    captureDate.year,
                    captureDate.month,
                    websiteConfiguration.locale
                )
            }

        val dayMatcher = monthMatcher
            .daySubcategoryMap
            .computeIfAbsent(captureDate.dayOfMonth) {
                DayMatcher(
                    monthMatcher.complexName,
                    captureDate.year,
                    captureDate.month,
                    captureDate.dayOfMonth,
                    websiteConfiguration.locale
                )
            }

        sequenceOf(yearMatcher, monthMatcher, dayMatcher).forEach {
            it.images += imageToProcess
            imageToProcess.categories += it.categoryName
        }
    }

    override fun toCategoryInformation(): CategoryInformation {
        return CategoryInformation(
            categoryName,
            images,
            subcategories.asSequence().map { it.toCategoryInformation() }.toSet(),
            false
        )
    }
}

private abstract class NoOpComputable : CategoryComputable {
    override fun matchImage(imageToProcess: InternalImageInformation, websiteConfiguration: WebsiteConfiguration) {
        error("No implementation needed as DateCategoryComputable does the computation")
    }
}

private class YearMatcher(baseName: String, year: Int) : NoOpComputable() {
    val complexName = "$baseName/$year"

    override val categoryName: CategoryName
        get() = CategoryName(complexName)
    override val images: MutableSet<InternalImageInformation> = concurrentSetOf()
    override val subcategories: MutableSet<CategoryComputable>
        get() = monthSubcategoryMap.values.toMutableSet()

    val monthSubcategoryMap = ConcurrentHashMap<Month, MonthMatcher>()

}

private class MonthMatcher(
    baseName: String,
    private val year: Int,
    private val month: Month,
    val locale: Locale
) : NoOpComputable() {
    val complexName = "$baseName/${month.value.toString().padStart(2, '0')}"

    override val categoryName: CategoryName
        get() = CategoryName(complexName, displayName = month.getDisplayName(TextStyle.FULL, locale) + " " + year)
    override val images: MutableSet<InternalImageInformation> = concurrentSetOf()
    override val subcategories: MutableSet<CategoryComputable>
        get() = daySubcategoryMap.values.toMutableSet()

    val daySubcategoryMap = ConcurrentHashMap<Int, DayMatcher>()


}


private class DayMatcher(
    baseName: String,
    private val year: Int,
    private val month: Month,
    day: Int,
    private val locale: Locale
) : NoOpComputable() {
    val dayString = day.toString().padStart(2, '0')

    private val complexName = "$baseName/$dayString"

    override val categoryName: CategoryName
        get() = CategoryName(
            complexName,
            displayName = dayString + ". " +
                    month.getDisplayName(TextStyle.FULL, locale) + " " +
                    year
        )
    override val images: MutableSet<InternalImageInformation> = concurrentSetOf()
    override val subcategories: MutableSet<CategoryComputable> = mutableSetOf()

}
