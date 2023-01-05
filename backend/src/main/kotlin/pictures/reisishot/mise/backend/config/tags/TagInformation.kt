package pictures.reisishot.mise.backend.config.tags

import kotlinx.serialization.Serializable
import pictures.reisishot.mise.commons.toUrlsafeString

@Serializable
class TagInformation(val name: String, val type: String = "MANUAL") : Comparable<TagInformation> {
    val url by lazy { name.toUrlsafeString().lowercase() }

    override fun compareTo(other: TagInformation): Int = name.compareTo(other.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagInformation

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "TagName(name='$name')"
    }
}
