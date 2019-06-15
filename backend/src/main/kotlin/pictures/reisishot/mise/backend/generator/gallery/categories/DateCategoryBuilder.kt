package pictures.reisishot.mise.backend.generator.gallery.categories

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.*
import pictures.reisishot.mise.backend.getAsZonedDateTime
import java.time.format.TextStyle

class DateCategoryBuilder(val rootCategoryName: String = "Kalendarisch") : CategoryBuilder {
    override val builderName: String = "Reisishot Kalender Category Builder"

    override suspend fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<FilenameWithoutExtension, CategoryName>> =
        imageInformationRepository.imageInformationData.asSequence()
            .flatMap {
                val captureDate = it.exifInformation.getAsZonedDateTime(ExifdataKey.CREATION_TIME)
                if (captureDate == null)
                    emptySequence()
                else
                    (it.filenameWithoutExtension to listOf<String>(
                        rootCategoryName,
                        captureDate.year.toString(),
                        captureDate.month.getDisplayName(TextStyle.FULL, websiteConfiguration.locale),
                        captureDate.dayOfMonth.toString()
                    )).allPossibleSublists()
            }


    private fun <A> Pair<A, List<CategoryName>>.allPossibleSublists(): Sequence<Pair<A, CategoryName>> {
        val tmp = mutableListOf<Pair<A, CategoryName>>()

        for (i in 1..second.size)
            tmp += first to arrayOf(*this.second.subList(0, i).toTypedArray()).joinToString("/")
        return tmp.asSequence()
    }
}