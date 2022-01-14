package pictures.reisishot.mise.backend

enum class Interpolation(val value: String) {
    NONE("4:4:4"),
    LOW("4:2:2"),
    MEDIUM("4:2:0"),
    HIGH("4:1:1")
}

interface ImageSize {
    val identifier: String
    val longestSidePx: Int
    val quality: Float
    val interpolation: Interpolation

    val smallerSize: ImageSize?
    val largerSize: ImageSize?
}

