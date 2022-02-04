package pictures.reisishot.mise.backend.config.category.computable

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pictures.reisishot.mise.backend.config.ImageInformation
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.backend.config.tags.computable.ExifTagComputable
import pictures.reisishot.mise.commons.ConcurrentSet
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.concurrentSetOf
import pictures.reisishot.mise.config.TestGermanLocaleProvider
import pictures.reisishot.mise.exifdata.ExifdataKey

class CameraLensCategoryComputableTest {

    @Test
    fun `Name must be trimmed`() {
        assertThat(CameraLensCategoryComputable(" Test ").complexName).isEqualTo("test")
    }

    @Test
    fun `Base name must be trimmed`() {
        assertThat(CameraLensCategoryComputable("Name", " base ").complexName).isEqualTo("base/name")
    }

    @Test
    fun `Name must not be blank`() {
        val ex = assertThrows<IllegalArgumentException> {
            CameraLensCategoryComputable("   ")
        }

        assertThat(ex).messageContains("blank")
    }

    @Test
    fun `Image with camera & lens exif data is detected correctly`() {
        val image = createImageInformation()
        val (_, computedSubcategory) = computeSubcategories(image)
        requireNotNull(computedSubcategory)
        assertThat(computedSubcategory.complexName).isEqualTo("test/canon-m50m2/ef-85mm-f-1.8-usm")
    }

    private fun computeSubcategories(image: ImageInformation) =
        CameraLensCategoryComputable("Test").let {
            it.matchImage(image, TestGermanLocaleProvider)
            val camera = it.subcategories.firstOrNull()
            val lens = camera
                ?.subcategories
                ?.firstOrNull()
            camera to lens
        }

    fun createImageInformation(
        cameraName: String? = "Canon M50m2",
        lensName: String? = "EF 85mm f/1.8 USM"
    ): ImageInformation {
        val tags = concurrentSetOf<TagInformation>().apply {
            cameraName?.let { add(TagInformation(it, ExifTagComputable.TAG_CAMERA)) }
            lensName?.let { add(TagInformation(it, ExifTagComputable.TAG_LENS)) }
        }

        return object : ImageInformation {
            override val filename: FilenameWithoutExtension = "Image"
            override val categories: ConcurrentSet<String> = concurrentSetOf()
            override val tags: ConcurrentSet<TagInformation> = tags
            override val exifInformation: Map<ExifdataKey, String> = emptyMap()

        }
    }
}
