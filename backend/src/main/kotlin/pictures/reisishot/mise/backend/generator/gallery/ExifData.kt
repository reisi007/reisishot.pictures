package pictures.reisishot.mise.backend.generator.gallery

import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Descriptor
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDescriptor
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.file.FileSystemDescriptor
import com.drew.metadata.file.FileSystemDirectory
import com.drew.metadata.jpeg.JpegDescriptor
import com.drew.metadata.jpeg.JpegDirectory
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class ExifdataKey(val displayName: String, val getValue: (ExifInformation) -> String?) {
    CAMERA_MAKE("Camera Make", { it.exifD0Descriptor?.cameraMakeDescription }),
    CAMERA_MODEL("Camera model", { it.exifD0Descriptor?.cameraModelDescription }),
    ISO("ISO", { it.exifSubIFDDescriptor?.isoEquivalentDescription }),
    APERTURE("Aperture", { it.exifSubIFDDescriptor?.apertureValueDescription }),
    SHUTTER_SPEED("Shutter speed", {
        it.exifSubIFDDescriptor?.let { desc ->
            desc.canonShutterspeedDescription ?: desc.shutterSpeedDescription
        }
    }),
    FOCAL_LENGTH("Focal length", { it.exifSubIFDDescriptor?.focalLengthDescription }),
    CREATION_TIME("Creation Time", {
        it.exifSubIFDDescriptor?.creationTimeDescription?.let {
            ZonedDateTime.of(LocalDateTime.from(exifDateTimeFormatter.parse(it)), ZoneId.systemDefault()).toString()
        }
    }),
    LENS_MODEL("Lens model", {
        it.exifSubIFDDescriptor?.lensModelDescription.let { lensName ->
            if (lensName.isNullOrBlank()) {
                val focalLength = FOCAL_LENGTH.getValue(it)
                if (!focalLength.isNullOrBlank())
                    return@let "$focalLength mm"
            }
            return@let lensName
        }
    });
}

private val exifDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

val ExifIFD0Descriptor.cameraMakeDescription: String? get() = getDescription(ExifIFD0Directory.TAG_MAKE)
val ExifIFD0Descriptor.cameraModelDescription: String? get() = getDescription(ExifIFD0Directory.TAG_MODEL)
val ExifSubIFDDescriptor.creationTimeDescription: String? get() = getDescription(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
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
        return "ExifInformation(jpegDescriptor=$jpegDescriptor, exifD0Descriptor=$exifD0Descriptor, exifSubIFDDescriptor=$exifSubIFDDescriptor, fileSystemDescriptor=$fileSystemDescriptor)"
    }

}