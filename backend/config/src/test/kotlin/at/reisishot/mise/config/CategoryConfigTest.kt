package at.reisishot.mise.config

import at.reisishot.mise.backend.config.LocaleProvider
import at.reisishot.mise.commons.testfixtures.softAssert
import at.reisishot.mise.commons.toTypedArray
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.category.*
import java.util.*

class CategoryConfigTest {

    @Test
    fun `category computation works`() = softAssert {
        val vowels = arrayOf("A", "E", "I", "O", "U")
        val charImages = generateAlphabet()
            .map { buildImageInformation(it) }
            .toList()

        val categoryConfig = buildCategoryConfig {
            withSubCategory("Chars") {
                val consonants = generateAlphabet() - vowels
                includeTagsAndSubcategories(*consonants.toTypedArray())
                withSubCategory("Vowels") {
                    includeTagsAndSubcategories(*vowels)
                }
            }
        }

        val computedCategories = categoryConfig.computeCategories(charImages)

        softAssert {
            val charsCategory = computedCategories.first()
            val vowelsCategory = charsCategory.subcategories.first()

            // Assert Vowels
            assertThat(vowelsCategory.tagsAsStringList).containsAll(listOf(*vowels))

            // Assert chars contains all chars
            assertThat(charsCategory.tagsAsStringList).containsAll(generateAlphabet().asIterable())
        }

    }

    private val CategoryInformation.tagsAsStringList
        get() = images.map { it.tags.first().name }


    private fun generateAlphabet() = (1..26).asSequence()
        .map { Char('A'.code + (it - 1)).toString() }

    private fun CategoryConfigRoot.computeCategories(
        charImages: List<ImageInformation>
    ) =
        computeCategoryInformation(
            charImages, object : LocaleProvider {
                override val locale: Locale = Locale.ENGLISH
            }
        )
}
