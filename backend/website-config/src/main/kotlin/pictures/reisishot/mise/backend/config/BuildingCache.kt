package pictures.reisishot.mise.backend.config

import pictures.reisishot.mise.commons.withChild
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BuildingCache {
    private lateinit var menuLinkPath: Path
    private lateinit var linkPath: Path

    companion object {
        fun getLinkFromFragment(
            config: WebsiteConfig,
            it: Link,
            websiteLocationSupplier: () -> String = { config.websiteInformation.normalizedWebsiteLocation }
        ): Link =
            if (it.startsWith("http", true))
                it
            else if (config.miseConfig.isDevMode)
                "http://localhost:3000/$it"
            else
                websiteLocationSupplier() + it
    }

    private val linkCache: MutableMap<String, MutableMap<String, Link>> = ConcurrentHashMap()

    private val internalMenuLinks: SortedSet<MenuLink> = TreeSet(compareBy(MenuLink::uniqueIndex, MenuLink::id))

    val menuLinks: Collection<MenuLink> get() = Collections.synchronizedCollection(internalMenuLinks)

    fun resetLinkcacheFor(linkType: String) = linkCache.computeIfAbsent(linkType) { ConcurrentHashMap() }.clear()

    fun addLinkcacheEntryFor(linkType: String, linkKey: String, link: Link) = synchronized(linkCache) {
        linkCache.computeIfAbsent(linkType) { ConcurrentHashMap() }.put(linkKey.lowercase(), link)
    }

    fun getLinkcacheEntryFor(config: WebsiteConfig, linkType: String, linkKey: String): Link =
        linkCache[linkType]?.get(linkKey)?.let {
            getLinkFromFragment(config, it)
        } ?: throw IllegalStateException("Menu link with type $linkType and key $linkKey not found!")

    fun getLinkcacheEntriesFor(linkType: String): Map<String, Link> = linkCache[linkType] ?: emptyMap()

    fun clearMenuItems(removePredicate: (MenuLink) -> Boolean) = synchronized(internalMenuLinks) {
        internalMenuLinks.removeIf(removePredicate)
    }

    fun addMenuItem(
        id: String = UUID.randomUUID().toString(),
        index: Int,
        href: Link,
        text: LinkText,
        target: String? = null
    ) =
        synchronized(internalMenuLinks) {
            val item = MenuLinkContainerItem(id, index, href, text, target)
            internalMenuLinks.add(item)
        }

    fun addMenuItemInContainerNoDupes(
        containerId: String,
        containerText: String,
        containerIndex: Int,
        text: LinkText,
        link: Link,
        target: String? = null,
        elementIndex: Int = 0
    ): Unit = synchronized(internalMenuLinks) {
        internalMenuLinks.find {
            it is MenuLinkContainer && it.id == containerId && it.uniqueIndex == containerIndex && it.children.any { child ->
                child.text == text && child.href == link
            }
        }.let {
            if (it == null)
                addMenuItemInContainer(
                    containerId,
                    containerText,
                    containerIndex,
                    text,
                    link,
                    target,
                    elementIndex
                )
        }
    }

    fun addMenuItemInContainer(
        containerId: String,
        containerText: String,
        containerIndex: Int,
        text: LinkText,
        link: Link,
        target: String? = null,
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
            text,
            target
        )

    }

    fun loadCache(config: WebsiteConfig) {
        config.useJsonParser {

            config.paths.cacheFolder.apply {
                menuLinkPath = withChild("menueItems.cache.json")
                linkPath = withChild("links.cache.json")
            }

            menuLinkPath.fromJson<List<MenuLink>>()?.let {
                internalMenuLinks += it
            }

            linkPath.fromJson<Map<String, MutableMap<String, String>>>()?.let {
                linkCache += it
            }
        }
    }

    fun saveCache(websiteConfig: WebsiteConfig) = websiteConfig.useJsonParser {
        internalMenuLinks.toList().toJson(menuLinkPath)
        linkCache.toJson(linkPath)
    }
}
