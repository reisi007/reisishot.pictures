package pictures.reisishot.mise.backend.generator.thumbnail

import kotlinx.serialization.Serializable

@Serializable
data class ImageSizeInformation(val filename: String, val width: Int, val height: Int)
