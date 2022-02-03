package pictures.reisishot.mise.config

import at.reisishot.mise.commons.testfixtures.softAssert
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.LocaleProvider
import pictures.reisishot.mise.backend.config.category.*
import pictures.reisishot.mise.commons.toTypedArray
import java.util.*


class CategoryConfigTest {
    @Test
    fun `category computation works`() = softAssert {
        val vowels = arrayOf("A", "E", "I", "O", "U")
        val consonants = generateAlphabet() - vowels
        val charImages = generateAlphabet()
            .map { buildImageInformation(it) }
            .toList()

        val categoryConfig = buildCategoryConfig {
            withSubCategory("Chars") {
                includeSubcategories()
                withSubCategory(CATEGORY_VOWELS) {
                    includeTagsAndSubcategories(*vowels)
                }

                withSubCategory(CATEGORY_CONSONANTS) {
                    includeTagsAndSubcategories(*consonants.toTypedArray())
                }
            }
        }

        val computedCategories = categoryConfig.computeCategories(charImages)

        softAssert {
            val charsCategory = computedCategories.first()
            val vowelsCategory = charsCategory.subcategories.first { it.categoryName.displayName == CATEGORY_VOWELS }
            val consonantCategory =
                charsCategory.subcategories.first { it.categoryName.displayName == CATEGORY_CONSONANTS }

            // Assert Vowels
            assertThat(vowelsCategory.tagsAsStringList).containsAll(listOf(*vowels))

            // Assert consonants
            assertThat(consonantCategory.tagsAsStringList).containsAll(consonants.toList())

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

    companion object {
        private const val CATEGORY_VOWELS = "Vowels"
        private const val CATEGORY_CONSONANTS = "Consonants"
    }
}
