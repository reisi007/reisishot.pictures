package pictures.reisishot.mise.exifdata

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Descriptor
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDescriptor
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.file.FileSystemDescriptor
import com.drew.metadata.file.FileSystemDirectory
import com.drew.metadata.jpeg.JpegDescriptor
import com.drew.metadata.jpeg.JpegDirectory
import com.drew.metadata.webp.WebpDirectory
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class ExifdataKey(val getValue: (ExifInformation) -> String?) {
    CAMERA_MAKE({ it.exifD0Descriptor?.cameraMakeDescription }),
    CAMERA_MODEL({ it.exifD0Descriptor?.cameraModelDescription }),
    LENS_MODEL({
        it.exifSubIFDDescriptor?.lensModelDescription.let { lensName ->
            if (lensName.isNullOrBlank()) {
                val focalLength = FOCAL_LENGTH.getValue(it)
                if (!focalLength.isNullOrBlank())
                    return@let "$focalLength mm"
            }
            return@let lensName
        }
    }),
    FOCAL_LENGTH({ it.exifSubIFDDescriptor?.focalLengthDescription }),
    CREATION_DATETIME({
        it.exifSubIFDDescriptor?.creationTimeDescription?.let { creationTime ->
            ZonedDateTime.of(LocalDateTime.from(exifDateTimeFormatter.parse(creationTime)), ZoneId.systemDefault())
                .toString()
        }
    }),
    APERTURE({ it.exifSubIFDDescriptor?.apertureValueDescription }),
    SHUTTER_SPEED({
        it.exifSubIFDDescriptor?.let { desc ->
            desc.canonShutterspeedDescription ?: desc.shutterSpeedDescription
        }
    }),
    ISO({ it.exifSubIFDDescriptor?.isoEquivalentDescription }),
}

private val exifDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

val ExifIFD0Descriptor.cameraMakeDescription: String? get() = getDescription(ExifIFD0Directory.TAG_MAKE)
val ExifIFD0Descriptor.cameraModelDescription: String? get() = getDescription(ExifIFD0Directory.TAG_MODEL)
val ExifSubIFDDescriptor.creationTimeDescription: String?
    get() = getDescription(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
val ExifSubIFDDescriptor.lensModelDescription: String? get() = getDescription(ExifSubIFDDirectory.TAG_LENS_MODEL)
val ExifSubIFDDescriptor.canonShutterspeedDescription: String? get() = getDescription(33434)

class ExifInformation(metadata: Metadata) {
    val jpegDescriptor: JpegDescriptor?
    val exifD0Descriptor: ExifIFD0Descriptor?
    val exifSubIFDDescriptor: ExifSubIFDDescriptor?
    val fileSystemDescriptor: FileSystemDescriptor?

    init {
        jpegDescriptor = metadata.getFirstDirectoryOfType(JpegDirectory::class.java)?.let { JpegDescriptor(it) }
        exifD0Descriptor =
            metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)?.let { ExifIFD0Descriptor(it) }
        exifSubIFDDescriptor =
            metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)?.let { ExifSubIFDDescriptor(it) }
        fileSystemDescriptor =
            metadata.getFirstDirectoryOfType(FileSystemDirectory::class.java)?.let { FileSystemDescriptor(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExifInformation

        if (jpegDescriptor != other.jpegDescriptor) return false
        if (exifD0Descriptor != other.exifD0Descriptor) return false
        if (exifSubIFDDescriptor != other.exifSubIFDDescriptor) return false
        if (fileSystemDescriptor != other.fileSystemDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jpegDescriptor?.hashCode() ?: 0
        result = 31 * result + (exifD0Descriptor?.hashCode() ?: 0)
        result = 31 * result + (exifSubIFDDescriptor?.hashCode() ?: 0)
        result = 31 * result + (fileSystemDescriptor?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ExifInformation(jpegDescriptor=$jpegDescriptor, exifD0Descriptor=$exifD0Descriptor," +
            " exifSubIFDDescriptor=$exifSubIFDDescriptor, fileSystemDescriptor=$fileSystemDescriptor)"
    }
}

val defaultExifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { cur ->
    when (cur.first) {
        ExifdataKey.LENS_MODEL -> {
            mapLensModel(cur)
        }
        ExifdataKey.CAMERA_MODEL -> {
            when (cur.second) {
                "Canon EOS M50m2" -> ExifdataKey.CAMERA_MODEL to "Canon EOS M50 Mark II"
                else -> cur
            }
        }
        else -> cur
    }
}

private fun mapLensModel(cur: Pair<ExifdataKey, String?>) =
    when (val value = cur.second) {
        "105.0 mm", "105mm", "105 mm", "105 mm mm" -> ExifdataKey.LENS_MODEL to "Sigma 105mm EX DG OS HSM"
        "147.0 mm", "147mm", "147 mm", "147 mm mm" ->
            ExifdataKey.LENS_MODEL to
                "Sigma 105mm EX DG OS HSM + 1.4 Sigma EX APO DG Telekonverter"
        "56mm F1.4 DC DN" -> ExifdataKey.LENS_MODEL to "Sigma $value"
        else -> if (value != null && value.contains(" |"))
            cur.first to value.substringBefore(" |")
        else cur
    }

fun Path.readExif(exifReplaceFunction: (Pair<ExifdataKey, String?>) -> Pair<ExifdataKey, String?> = { it }): MutableMap<ExifdataKey, String> =
    mutableMapOf<ExifdataKey, String>().apply {
        ExifInformation(ImageMetadataReader.readMetadata(this@readExif.toFile()))
            .let { exifInformation ->
                ExifdataKey.values().forEach { key ->
                    val exifValue = key.getValue(exifInformation)
                    exifReplaceFunction(key to exifValue)
                        .also { (key, possibleValue) ->
                            if (possibleValue != null)
                                put(key, possibleValue)
                        }
                }
            }
    }

val WebpDirectory.width
    get() = getInt(WebpDirectory.TAG_IMAGE_WIDTH)

val WebpDirectory.height
    get() = getInt(WebpDirectory.TAG_IMAGE_HEIGHT)
