package pictures.reisishot.mise.backend.config

import pictures.reisishot.mise.commons.withChild
import java.nio.file.Path
import java.util.Collections
import java.util.SortedSet
import java.util.TreeSet
import java.util.UUID
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

    private val internalLinkCache: MutableMap<String, MutableMap<String, Link>> = ConcurrentHashMap()

    val linkCache
        get() = internalLinkCache.asSequence()
            .map { (key, value) -> key to value.toMap() }
            .toMap()

    private val internalMenuLinks: SortedSet<MenuLink> = TreeSet(compareBy(MenuLink::uniqueIndex, MenuLink::id))

    val menuLinks: Collection<MenuLink> get() = Collections.synchronizedCollection(internalMenuLinks)

    fun resetLinkcacheFor(linkType: String) =
        internalLinkCache.computeIfAbsent(linkType) { ConcurrentHashMap() }.clear()

    fun addLinkcacheEntryFor(linkType: String, linkKey: String, link: Link) = synchronized(internalLinkCache) {
        internalLinkCache.computeIfAbsent(linkType) { ConcurrentHashMap() }.put(linkKey.lowercase(), link)
    }

    fun getLinkcacheEntryFor(config: WebsiteConfig, linkType: String, linkKey: String): Link =
        internalLinkCache[linkType]?.get(linkKey)?.let {
            getLinkFromFragment(config, it)
        } ?: throw IllegalStateException("Menu link with type $linkType and key $linkKey not found!")

    fun getLinkcacheEntriesFor(linkType: String): Map<String, Link> = internalLinkCache[linkType] ?: emptyMap()

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

    fun addMenuItemInExistingContainer(
        containerId: String,
        text: LinkText,
        link: Link,
        target: String?,
        elementIndex: Int = 0
    ) {
        val container = findMenuContainer(containerId)
            ?: throw IllegalStateException("Container with id $containerId does not exist")

        container += MenuLinkContainerItem(
            UUID.randomUUID().toString(),
            elementIndex,
            link,
            text,
            target
        )
    }

    fun addMenuItemInContainer(
        containerId: String,
        containerText: String,
        containerIndex: Int,
        text: LinkText,
        link: Link,
        target: String? = null,
        elementIndex: Int
    ) = synchronized(internalMenuLinks) {
        val menuLinkContainer = findMenuContainer(containerId) ?: run {
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

    private fun findMenuContainer(containerId: String) = internalMenuLinks.find {
        it is MenuLinkContainer && containerId == it.id
    } as? MenuLinkContainer

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
                internalLinkCache += it
            }
        }
    }

    fun saveCache(websiteConfig: WebsiteConfig) = websiteConfig.useJsonParser {
        internalMenuLinks.toList().toJson(menuLinkPath)
        internalLinkCache.toJson(linkPath)
    }
}
