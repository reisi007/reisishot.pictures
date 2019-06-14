package pictures.reisishot.mise.backend.generator.gallery.categories

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.*
import java.time.LocalDateTime
import java.time.format.TextStyle

class DateCategoryBuilder(val rootCategoryName: String = "Kalendarisch") : CategoryBuilder {
    override val builderName: String = "Reisishot Kalender Category Builder"

    override suspend fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<Pair<ImageFilename, CategoryName>> =
        imageInformationRepository.allImageInformationData.asSequence()
            .flatMap {
                val captureDate = LocalDateTime.parse(it.exifInformation[ExifdataKey.CREATION_TIME])
                (it.filename to listOf<String>(
                    rootCategoryName,
                    captureDate.year.toString(),
                    captureDate.month.getDisplayName(TextStyle.FULL, websiteConfiguration.locale),
                    captureDate.dayOfMonth.toString()
                )).allPossibleSublists()
            }

    private fun Pair<ImageFilename, List<String>>.allPossibleSublists(): Sequence<Pair<ImageFilename, CategoryName>> {
        val tmp = mutableListOf<Pair<ImageFilename, String>>()

        for (i in 1..second.size)
            tmp += first to arrayOf(first, *this.second.subList(0, i).toTypedArray()).joinToString("/")
        return tmp.asSequence()
    }
}