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

    fun addMenuItem(id: String = UUID.randomUUID().toString(), index: Int, href: Link, text: LinkText) {
        val item = MenuLinkContainerItem(id, index, href, text)
        internalMenuLinks.add(item)
    }

    fun addMenuItemInContainer(
        containerId: String,
        containerText: String,
        index: Int,
        text: LinkText,
        link: Link
    ) {
        val menuLinkContainer = internalMenuLinks.find {
            it is MenuLinkContainer && containerId == it.id
        } as? MenuLinkContainer ?: run {
            val newContainer = MenuLinkContainer(
                containerId,
                index,
                containerText
            )
            newContainer
        }
        menuLinkContainer += MenuLinkContainerItem(
            UUID.randomUUID().toString(),
            menuLinkContainer.children.size,
            link,
            text
        )
        internalMenuLinks.add(menuLinkContainer)

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