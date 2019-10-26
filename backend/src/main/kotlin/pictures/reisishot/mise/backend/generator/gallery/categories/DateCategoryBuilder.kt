package pictures.reisishot.mise.backend.generator.gallery.categories

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.*
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
                            (it.filenameWithoutExtension to listOf<String>(
                                    rootCategoryName,
                                    captureDate.year.toString(),
                                    captureDate.month.getDisplayName(TextStyle.FULL, websiteConfiguration.locale),
                                    captureDate.dayOfMonth.toString()
                            )).allPossibleSublists()
                    }


    private fun <A> Pair<A, List<String>>.allPossibleSublists(): Sequence<Pair<A, CategoryInformation>> {
        val tmp = mutableListOf<Pair<A, CategoryInformation>>()

        for (i in 1..second.size) {
            val urlFragments = this.second.subList(0, i)
            val nameFragments = arrayOf(*urlFragments.toTypedArray())
            tmp += first to CategoryInformation(
                    internalName = CategoryName(nameFragments.asSequence().drop(1).joinToString("/"), nameFragments.computeName()),
                    urlFragment = nameFragments.joinToString("/") { it }.substringAfter('/').toLowerCase(),
                    visible = false
            )
        }
        return tmp.asSequence()
    }

    private fun Array<String>.computeName() = let {
        when (it.size) {
            4 -> it[3] + ". " + it[2] + " " + it[1]
            3 -> it[2] + " " + it[1]
            2 -> it[1]
            1 -> it[0]
            else -> throw IllegalStateException("Count ${it.size} not expected!")
        }
    }

}