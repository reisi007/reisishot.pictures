package pictures.reisishot.mise.backend.generator

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.fromXml
import pictures.reisishot.mise.backend.toXml
import pictures.reisishot.mise.backend.withChild
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Function

class BuildingCache {


    private lateinit var menuLinkPath: Path
    private lateinit var linkPath: Path

    private val linkCache: MutableMap<String, MutableMap<String, Link>> = mutableMapOf()


    private val internalMenuLinks: SortedSet<MenuLink> =
        Collections.synchronizedSortedSet(TreeSet<MenuLink>(Comparator.comparing(Function<MenuLink, Int> { it.uniqueIndex })))

    val menuLinks: Set<MenuLink> get() = internalMenuLinks

    private val dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    fun resetLinkcacheFor(linkType: String) = linkCache.computeIfAbsent(linkType) { mutableMapOf() }.clear()


    fun addLinkcacheEntryFor(linkType: String, linkKey: String, link: Link) = synchronized(linkCache) {
        linkCache.computeIfAbsent(linkType) { mutableMapOf() }.put(linkKey, link)
    }


    fun clearMenuItems(removePredicate: (MenuLink) -> Boolean) {
        internalMenuLinks.removeAll(removePredicate)
    }

    fun addMenuItem(
        containerId: String? = null,
        containerText: String? = null,
        index: Int,
        text: LinkText,
        link: Link
    ) {
        if (containerId != null && containerText != null) {
            val menuLinkContainer = internalMenuLinks.find {
                it is MenuLinkContainer && containerId == it.containerId
            } as? MenuLinkContainer ?: kotlin.run {
                val newContainer = MenuLinkContainer(
                    containerId,
                    index,
                    containerText
                )
                newContainer
            }
            menuLinkContainer += MenuLinkContainerItem(menuLinkContainer.children.size, link, text)
            internalMenuLinks.add(menuLinkContainer)
        } else {
            sequenceOf(containerId, containerText)
                .filter { it != null }
                .any()
                .let { containerNeeded -> if (containerNeeded) throw IllegalStateException("Container needed, either all or no variables must be null!") }
            val item = MenuLinkContainerItem(index, link, text)


            internalMenuLinks.add(item)

        }
    }

    internal fun loadCache(config: WebsiteConfiguration) {
        with(config.tmpPath) {
            menuLinkPath = withChild("menueItems.cache.xml")
            linkPath = withChild("links.cache.xml")
        }



        menuLinkPath.fromXml<Set<MenuLink>>()?.let {
            internalMenuLinks += it
        }

        linkPath.fromXml<Map<String, MutableMap<String, String>>>()?.let {
            linkCache += it
        }
    }

    internal fun saveCache(config: WebsiteConfiguration) {
        internalMenuLinks.toXml(menuLinkPath)
        linkCache.toXml(linkPath)
    }
}