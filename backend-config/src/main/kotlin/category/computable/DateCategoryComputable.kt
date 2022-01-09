package pictures.reisishot.mise.backend.config.category.computable

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.commons.concurrentSetOf
import at.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.category.CategoryComputable
import pictures.reisishot.mise.backend.config.category.CategoryInformation
import pictures.reisishot.mise.backend.config.category.LocaleProvider
import pictures.reisishot.mise.backend.config.category.NoOpComputable
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DateCategoryComputable(
    name: String,
    baseName: String? = null,
    _defaultImages: () -> Map<Triple<Int?/*year*/, Month?, Int?/*Day*/>, FilenameWithoutExtension> = { emptyMap() }
) : CategoryComputable {
    private val defaultImages = _defaultImages()
    override val complexName: String = (if (baseName == null) "" else "$baseName/") + name
    private val yearSubcategoryMap = ConcurrentHashMap<Int, YearMatcher>()
    override val subcategories: MutableSet<CategoryComputable>
        get() = yearSubcategoryMap.values.toMutableSet()

    override val images: MutableSet<ImageInformation> = concurrentSetOf()
    override val categoryName by lazy { CategoryName(complexName) }
    override val defaultImage: FilenameWithoutExtension? = defaultImages[Triple(null, null, null)]

    override fun matchImage(imageToProcess: ImageInformation, localeProvider: LocaleProvider) {
        val captureDate = imageToProcess.exifInformation[ExifdataKey.CREATION_DATETIME]
            ?.let { ZonedDateTime.parse(it) } ?: return

        val yearMatcher = yearSubcategoryMap
            .computeIfAbsent(captureDate.year) {
                YearMatcher(
                    complexName,
                    captureDate.year,
                    defaultImages
                )
            }

        val monthMatcher = yearMatcher
            .monthSubcategoryMap
            .computeIfAbsent(captureDate.month) {
                MonthMatcher(
                    yearMatcher.complexName,
                    captureDate.year,
                    captureDate.month,
                    defaultImages,
                    localeProvider.locale
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
                    defaultImages,
                    localeProvider.locale
                )
            }

        sequenceOf(this, yearMatcher, monthMatcher, dayMatcher).forEach {
            it.images += imageToProcess
            imageToProcess.categories += it.complexName
        }
    }

    override fun toCategoryInformation(): CategoryInformation {
        return CategoryInformation(
            categoryName,
            images,
            images.lastOrNull(),
            subcategories.asSequence().map { it.toCategoryInformation() }.toSet(),
            false
        )
    }
}

private class YearMatcher(
    baseName: String,
    year: Int,
    defaultImages: Map<Triple<Int?, Month?, Int?>, FilenameWithoutExtension>
) : NoOpComputable() {
    override val complexName = "$baseName/$year"

    override val categoryName: CategoryName by lazy { CategoryName(complexName) }
    override val images: MutableSet<ImageInformation> = concurrentSetOf()
    override val defaultImage: FilenameWithoutExtension? by lazy {
        defaultImages[Triple(year, null, null)]
    }
    override val subcategories: MutableSet<CategoryComputable>
        get() = monthSubcategoryMap.values.toMutableSet()

    val monthSubcategoryMap = ConcurrentHashMap<Month, MonthMatcher>()

}

private class MonthMatcher(
    baseName: String,
    private val year: Int,
    private val month: Month,
    defaultImages: Map<Triple<Int?, Month?, Int?>, FilenameWithoutExtension>,
    val locale: Locale
) : NoOpComputable() {
    override val complexName = "$baseName/${month.value.toString().padStart(2, '0')}"

    override val categoryName: CategoryName by lazy {
        CategoryName(
            complexName,
            displayName = month.getDisplayName(TextStyle.FULL, locale) + " " + year
        )
    }
    override val images: MutableSet<ImageInformation> = concurrentSetOf()
    override val defaultImage: FilenameWithoutExtension? by lazy {
        defaultImages[Triple(year, month, null)]
    }
    override val subcategories: MutableSet<CategoryComputable>
        get() = daySubcategoryMap.values.toMutableSet()

    val daySubcategoryMap = ConcurrentHashMap<Int, DayMatcher>()
}

private class DayMatcher(
    baseName: String,
    private val year: Int,
    private val month: Month,
    day: Int,
    defaultImages: Map<Triple<Int?, Month?, Int?>, String>,
    private val locale: Locale
) : NoOpComputable() {
    val dayString = day.toString().padStart(2, '0')

    override val complexName = "$baseName/$dayString"

    override val categoryName: CategoryName by lazy {
        CategoryName(
            complexName,
            displayName = dayString + ". " +
                    month.getDisplayName(TextStyle.FULL, locale) + " " +
                    year
        )
    }

    override val images: MutableSet<ImageInformation> = concurrentSetOf()
    override val defaultImage: FilenameWithoutExtension? by lazy {
        defaultImages[Triple(year, month, day)]
    }
    override val subcategories: MutableSet<CategoryComputable> = mutableSetOf()
}
