package pictures.reisishot.mise.backend.config.tags.computable

import pictures.reisishot.mise.backend.config.ExtImageInformation
import pictures.reisishot.mise.backend.config.TagConfigDsl
import pictures.reisishot.mise.backend.config.tags.TagComputable
import pictures.reisishot.mise.backend.config.tags.TagConfig
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.exifdata.ExifdataKey

class ExifTagComputable : TagComputable {

    companion object {
        const val TAG_CAMERA = "CAMERA"
        const val TAG_LENS = "LENS"
    }

    override fun processImage(imageInformation: ExtImageInformation) {
        val camera = imageInformation.exifInformation[ExifdataKey.CAMERA_MODEL]
        val lens = imageInformation.exifInformation[ExifdataKey.LENS_MODEL]
        if (camera == null || lens == null) return

        // Add tags
        imageInformation.tags += arrayOf(
            TagInformation(camera, TAG_CAMERA),
            TagInformation(lens, TAG_LENS)
        )
    }
}

@TagConfigDsl
fun TagConfig.computeTagsFromExif() = withComputable {
    ExifTagComputable()
}
