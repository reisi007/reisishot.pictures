package pictures.reisishot.mise.backend.config.tags.computable

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import pictures.reisishot.mise.config.buildImageInformation
import pictures.reisishot.mise.exifdata.ExifdataKey


class ExifTagComputableTest {

    companion object {
        private const val EXISTING_TAG = "Existing Tag"
        private const val CAMERA = "Canon M50m2"
        private const val LENS = "85mm f/1.8"
    }

    @Test
    fun `Category and lens tags can be inferred from EXIF`() {
        val image = buildImageInformation(
            setOf(EXISTING_TAG),
            mapOf(
                ExifdataKey.CAMERA_MODEL to CAMERA,
                ExifdataKey.LENS_MODEL to LENS
            )
        )

        val computable = ExifTagComputable()
        computable.processImage(image)

        assertThat(image.tags.map { it.name }).containsAll(EXISTING_TAG, CAMERA, LENS)
        assertThat(image.tags.first { it.type == ExifTagComputable.TAG_LENS }.name).isEqualTo(LENS)
        assertThat(image.tags.first { it.type == ExifTagComputable.TAG_CAMERA }.name).isEqualTo(CAMERA)
    }
}
