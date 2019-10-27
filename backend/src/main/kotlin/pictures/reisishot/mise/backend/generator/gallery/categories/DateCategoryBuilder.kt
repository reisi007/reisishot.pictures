package pictures.reisishot.mise.backend.generator.gallery.categories

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.*
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.TextStyle

class DateCategoryBuilder(val rootCategoryName: String) : CategoryBuilder {
    override val builderName: String = "Reisishot Kalender Category Builder"

    override suspend fun generateCategories(
            imageInformationRepository: ImageInformationRepository,
            websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<FilenameWithoutExtension, CategoryInformation>> =
            imageInformationRepository.imageInformationData.asSequence()
                    .flatMap {
                        val captureDate = it.exifInformation.get(ExifdataKey.CREATION_TIME)?.let { ZonedDateTime.parse(it) }
                        if (captureDate == null)
                            emptySequence()
                        else
                            (it.filenameWithoutExtension to DateCategoryData(
                                    rootCategoryName,
                                    captureDate.year.toString(),
                                    captureDate.month,
                                    captureDate.dayOfMonth.toString()
                            )).mapToCategory(websiteConfiguration)
                    }

    private fun <A> Pair<A, DateCategoryData>.mapToCategory(websiteConfiguration: WebsiteConfiguration): Sequence<Pair<A, CategoryInformation>> {
        val tmp = mutableListOf<Pair<A, CategoryInformation>>()

        for (i in 1..4) {
            val internalName = CategoryName(
                    complexName = second.computeComplexName(websiteConfiguration, i),
                    displayName = second.computeDisplayName(websiteConfiguration, i)
            )
            tmp += first to CategoryInformation(
                    internalName,
                    internalName.complexName.substringAfter('/').toLowerCase(),
                    false,
                    if (i == 1) chronologisch else getDateCategoryBuilder(internalName.complexName)
            )
        }
        return tmp.asSequence()
    }

    data class DateCategoryData(val root: String, val year: String, val month: Month, val day: String);
    private fun DateCategoryData.computeDisplayName(websiteConfiguration: WebsiteConfiguration, depth: Int): String = let {
        when (depth) {
            4 -> day + ". " + computeDisplayName(websiteConfiguration, 3)
            3 -> month.toPrettyString(websiteConfiguration) + " " + computeDisplayName(websiteConfiguration, 2)
            2 -> year
            1 -> root
            else -> throw IllegalStateException("Count ${depth} not expected!")
        }
    }

    private fun DateCategoryData.computeComplexName(websiteConfiguration: WebsiteConfiguration, depth: Int): ComplexName = let {
        when (depth) {
            4 -> computeComplexName(websiteConfiguration, 3) + "/" + day
            3 -> computeComplexName(websiteConfiguration, 2) + "/" + month.value.toString().padStart(2, '0')
            2 -> year
            1 -> root
            else -> throw IllegalStateException("Count ${depth} not expected!")
        }
    }

    private fun Month.toPrettyString(websiteConfiguration: WebsiteConfiguration) = getDisplayName(TextStyle.FULL, websiteConfiguration.locale)

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