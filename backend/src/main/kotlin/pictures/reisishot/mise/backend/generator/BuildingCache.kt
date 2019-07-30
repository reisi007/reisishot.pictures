package pictures.reisishot.mise.backend.generator

import com.google.common.collect.SortedMultiset
import com.google.common.collect.TreeMultiset
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.fromXml
import pictures.reisishot.mise.backend.toXml
import pictures.reisishot.mise.backend.withChild
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import java.util.*

class BuildingCache {
    private lateinit var menuLinkPath: Path
    private lateinit var linkPath: Path

    private val linkCache: MutableMap<String, MutableMap<String, Link>> = mutableMapOf()


    private val internalMenuLinks: SortedMultiset<MenuLink> =
        TreeMultiset.create(Comparator.comparing<MenuLink, Int> { it.uniqueIndex })

    val menuLinks: Collection<MenuLink> get() = Collections.synchronizedCollection(internalMenuLinks)

    private val dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    fun resetLinkcacheFor(linkType: String) = linkCache.computeIfAbsent(linkType) { mutableMapOf() }.clear()


    fun addLinkcacheEntryFor(linkType: String, linkKey: String, link: Link) = synchronized(linkCache) {
        linkCache.computeIfAbsent(linkType) { mutableMapOf() }.put(linkKey, link)
    }

    fun getLinkcacheEntryFor(linkType: String, linkKey: String): Link = linkCache[linkType]?.get(linkKey)
        ?: throw IllegalStateException("Menu link with type $linkType and key $linkKey not found!")

    fun clearMenuItems(removePredicate: (MenuLink) -> Boolean) = synchronized(internalMenuLinks) {
        internalMenuLinks.removeIf(removePredicate)
    }

    fun addMenuItem(id: String = UUID.randomUUID().toString(), index: Int, href: Link, text: LinkText) =
        synchronized(internalMenuLinks) {
            val item = MenuLinkContainerItem(id, index, href, text)
            internalMenuLinks.add(item)
        }

    fun addMenuItemInContainerNoDupes(
        containerId: String,
        containerText: String,
        containerIndex: Int,
        text: LinkText,
        link: Link,
        elementIndex: Int = 0
    ): Unit = synchronized(internalMenuLinks) {
        internalMenuLinks.find {
            it is MenuLinkContainer && it.id == containerId && it.text == containerText
                    && it.uniqueIndex == containerIndex && it.children.any {
                it.text == text && it.href == link
            }
        }.let {
            if (it == null)
                addMenuItemInContainer(containerId, containerText, containerIndex, text, link, elementIndex)
        }
    }

    fun addMenuItemInContainer(
        containerId: String,
        containerText: String,
        containerIndex: Int,
        text: LinkText,
        link: Link,
        elementIndex: Int = 0
    ) = synchronized(internalMenuLinks) {
        val menuLinkContainer = internalMenuLinks.find {
            it is MenuLinkContainer && containerId == it.id
        } as? MenuLinkContainer ?: run {
            val newContainer = MenuLinkContainer(
                containerId,
                containerIndex,
                containerText
            )
            internalMenuLinks.add(newContainer)
            newContainer
        }
        menuLinkContainer += MenuLinkContainerItem(
            UUID.randomUUID().toString(),
            elementIndex,
            link,
            text
        )

    }

    internal fun loadCache(config: WebsiteConfiguration) {
        with(config.tmpPath) {
            menuLinkPath = withChild("menueItems.cache.xml")
            linkPath = withChild("links.cache.xml")
        }



        menuLinkPath.fromXml<List<MenuLink>>()?.let {
            internalMenuLinks += it
        }

        linkPath.fromXml<Map<String, MutableMap<String, String>>>()?.let {
            linkCache += it
        }
    }

    internal fun saveCache(config: WebsiteConfiguration) {
        internalMenuLinks.toList().toXml(menuLinkPath)
        linkCache.toXml(linkPath)
    }
}