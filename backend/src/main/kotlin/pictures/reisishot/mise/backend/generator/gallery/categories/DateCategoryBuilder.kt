package pictures.reisishot.mise.backend.generator.gallery.categories

import at.reisishot.mise.commons.CategoryName
import at.reisishot.mise.commons.ComplexName
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.*
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.TextStyle


class DateCategoryBuilder(val rootCategoryName: String) : CategoryBuilder {
    override val builderName: String = "Reisishot Kalender Category Builder"

    private enum class DateCategoryTypes {
        ROOT, YEAR, MONTH, DAY;
    }

    override suspend fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<FilenameWithoutExtension, CategoryInformation>> =
        imageInformationRepository.imageInformationData.asSequence()
            .map { it as? InternalImageInformation }
            .filterNotNull()
            .flatMap {
                val captureDate = it.exifInformation.get(ExifdataKey.CREATION_DATETIME)?.let { ZonedDateTime.parse(it) }
                if (captureDate == null)
                    emptySequence()
                else
                    (it.filename to DateCategoryData(
                        rootCategoryName,
                        captureDate.year.toString(),
                        captureDate.month,
                        captureDate.dayOfMonth.toString()
                    )).mapToCategory(websiteConfiguration)
            }

    private fun <A> Pair<A, DateCategoryData>.mapToCategory(websiteConfiguration: WebsiteConfiguration): Sequence<Pair<A, CategoryInformation>> {
        val tmp = mutableListOf<Pair<A, CategoryInformation>>()

        DateCategoryTypes.values().forEach {
            val internalName = CategoryName(
                complexName = second.computeComplexName(websiteConfiguration, it),
                displayName = second.computeDisplayName(websiteConfiguration, it)
            )
            tmp += first to CategoryInformation(
                internalName,
                internalName.complexName.toLowerCase(),
                false,
                if (it == DateCategoryTypes.ROOT) chronologisch else getDateCategoryBuilder(internalName.complexName)
            )
        }
        return tmp.asSequence()
    }

    data class DateCategoryData(val root: String, val year: String, val month: Month, val day: String)

    private fun DateCategoryData.computeDisplayName(
        websiteConfiguration: WebsiteConfiguration,
        depth: DateCategoryTypes
    ): String = when (depth) {
        DateCategoryTypes.DAY -> day.padStart(2, '0') + ". " + computeDisplayName(
            websiteConfiguration,
            DateCategoryTypes.MONTH
        )
        DateCategoryTypes.MONTH -> month.toPrettyString(websiteConfiguration) + " " + computeDisplayName(
            websiteConfiguration,
            DateCategoryTypes.YEAR
        )
        DateCategoryTypes.YEAR -> year
        DateCategoryTypes.ROOT -> root
    }


    private fun DateCategoryData.computeComplexName(
        websiteConfiguration: WebsiteConfiguration,
        depth: DateCategoryTypes
    ): ComplexName = when (depth) {
        DateCategoryTypes.DAY -> computeComplexName(websiteConfiguration, DateCategoryTypes.MONTH) + "/" + day.padStart(
            2,
            '0'
        )
        DateCategoryTypes.MONTH -> computeComplexName(
            websiteConfiguration,
            DateCategoryTypes.YEAR
        ) + "/" + month.value.toString().padStart(2, '0')
        DateCategoryTypes.YEAR -> year
        DateCategoryTypes.ROOT -> root
    }


    private fun Month.toPrettyString(websiteConfiguration: WebsiteConfiguration) =
        getDisplayName(TextStyle.FULL, websiteConfiguration.locale)

    private val chronologisch: SubcategoryComputator = {
        (it[0]?.asSequence() ?: emptySequence())
            .filter { it.displayName.toIntOrNull() != null }
            .map { it.internalName }
            .toSet()
    }

    private fun getDateCategoryBuilder(curCategoryName: ComplexName): SubcategoryComputator =
        curCategoryName.count { it == '/' }.let { categoryLevel ->
            return@let { map ->
                (map[categoryLevel + 1]?.asSequence() ?: emptySequence())
                    .filter { it.complexName.startsWith(curCategoryName) }
                    .map { it.internalName }
                    .toSet()
            }
        }
}
