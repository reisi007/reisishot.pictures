package pictures.reisishot.mise.backend.generator

import pictures.reisishot.mise.backend.*
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Function

object BuildingCache {

    private lateinit var timestampMapPath: Path
    private lateinit var menuLinkPath: Path
    private lateinit var linkPath: Path

    private val linkCahce: MutableMap<String, MutableMap<String, Link>> = mutableMapOf()


    private val internalMenuLinks: SortedSet<MenuLink> =
        Collections.synchronizedSortedSet(TreeSet<MenuLink>(Comparator.comparing(Function<MenuLink, Int> { it.uniqueIndex })))

    val menuLinks: Set<MenuLink> get() = internalMenuLinks

    // Used for querying (all plugins should have the same cache
    private val oldtimestampMap: MutableMap<String, ZonedDateTime> = mutableMapOf()
    // Used for updating the cache for the next run
    private val timestampMap: MutableMap<String, ZonedDateTime> = mutableMapOf()


    private val dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    fun resetLinkcacheFor(linkType: String) = linkCahce.computeIfAbsent(linkType) { mutableMapOf() }.clear()


    fun addLinkcacheEntryFor(linkType: String, linkKey: String, link: Link) = synchronized(linkCahce) {
        linkCahce.computeIfAbsent(linkType) { mutableMapOf() }.put(linkKey, link)
    }

    // TODO Will be moved to Thuimbnails plugin in the future
    fun hasFileChanged(p: Path): Boolean {
        val cachedValue: ZonedDateTime? = oldtimestampMap.get(p.toNormalizedString())

        val actualValue = p.fileModifiedDateTime

        val hasChanged = when {
            cachedValue == null && actualValue != null -> {
                // New file
                true
            }
            cachedValue != null && actualValue == null -> {
                // File deleted
                true
            }
            actualValue != null && actualValue > cachedValue -> {
                // File changed
                true
            }
            else -> false
        }
        return hasChanged
    }

    fun setFileChanged(p: Path, time: ZonedDateTime?) = with(p.toNormalizedString()) {
        synchronized(timestampMap) {
            if (time != null)
                timestampMap.put(this, time)
            else
                timestampMap.remove(this)
        }
    }

    fun clearMenuItems(removePredicate: (MenuLink) -> Boolean) {
        internalMenuLinks.removeAll(removePredicate)
    }

    fun addMenuItem(
        containerType: String? = null,
        containerText: String? = null,
        index: Int,
        text: LinkText,
        link: Link
    ) {
        if (containerType != null && containerText != null) {
            val menuLinkContainer = internalMenuLinks.find {
                it is MenuLinkContainer && containerType == it.containerId
            } as? MenuLinkContainer ?: kotlin.run {
                val newContainer = MenuLinkContainer(
                    containerType,
                    index,
                    containerText
                )
                newContainer
            }
            menuLinkContainer += MenuLinkContainerItem(menuLinkContainer.children.size, link, text)
            internalMenuLinks.add(menuLinkContainer)
        } else {
            sequenceOf(containerType, containerText)
                .filter { it != null }
                .any()
                .let { containerNeeded -> if (containerNeeded) throw IllegalStateException("Container needed, either all or no variables must be null!") }
            val item = MenuLinkContainerItem(index, link, text)


            internalMenuLinks.add(item)

        }
    }

    internal fun loadCache(config: WebsiteConfiguration) {
        with(config.tmpPath) {
            timestampMapPath = withChild("timestamp.cache.xml")
            menuLinkPath = withChild("menueItems.cache.xml")
            linkPath = withChild("links.cache.xml")
        }

        timestampMapPath.fromXml<MutableMap<String, ZonedDateTime>>()?.let {
            oldtimestampMap += it
            timestampMap += it
        }

        menuLinkPath.fromXml<Set<MenuLink>>()?.let {
            internalMenuLinks += it
        }

        linkPath.fromXml<Map<String, MutableMap<String, String>>>()?.let {
            linkCahce += it
        }
    }

    internal fun saveCache(config: WebsiteConfiguration) {
        timestampMap.toXml(timestampMapPath)
        internalMenuLinks.toXml(menuLinkPath)
        linkCahce.toXml(linkPath)
    }
}