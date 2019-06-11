package pictures.reisishot.mise.backend.generator.gallery.categories

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.CategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.CategoryName
import pictures.reisishot.mise.backend.generator.gallery.ImageInformationRepository
import java.time.LocalDateTime
import java.time.format.TextStyle

class DateCategoryBuilder(val rootCategoryName: String = "Kalendarisch") : CategoryBuilder {
    override val builderName: String = "Reisishot Kalender Category Builder"

    override fun generateCategories(
        imageInformationRepository: ImageInformationRepository,
        websiteConfiguration: WebsiteConfiguration
    ): Sequence<CategoryName> =
        imageInformationRepository.allImageInformationData.asSequence()
            .flatMap {
                //TODO pseudo code
                val captureDate = LocalDateTime.parse(it.exifInformation["date"])
                listOf<String>(
                    rootCategoryName,
                    captureDate.year.toString(),
                    captureDate.month.getDisplayName(TextStyle.FULL, websiteConfiguration.locale),
                    captureDate.dayOfMonth.toString()
                ).allPossibleSublists()
            }

    private fun <T> List<T>.allPossibleSublists(): Sequence<List<T>> {
        val tmp = mutableListOf<List<T>>()
        for (i in 1..size)
            tmp += this.subList(0, i)
        return tmp.asSequence()
    }
}