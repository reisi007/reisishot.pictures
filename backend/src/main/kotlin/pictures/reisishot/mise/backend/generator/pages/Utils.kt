package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.pages.minimalistic.SourcePath
import pictures.reisishot.mise.backend.generator.pages.minimalistic.TargetPath
import java.nio.file.Path
import java.util.*


data class PageInformation(
    val menuContainerName: String, val destinationPath: Path, val globalPriority: Int,
    val menuItemName: String, val menuItemDisplayName: String, val menuItemPriority: Int,
    val folderName: String, val folderDisplayName: String
)

private val displayReplacePattern = Regex("[\\-_]")
private val filenameParser =
    Regex("""^(?<globalPriority>\d+)--((?<menuContainerPriority>\d+)--(?<menuContainerName>.+?))?--(?<menuItemName>.+?)(--(?<folderName>.+?))?${'$'}""")

fun Path.computeMinimalInfo(
    generatorName: String,
    configuration: WebsiteConfiguration,
    cache: BuildingCache
): PageMinimalInfo {
    configuration.inPath.relativize(this).let { filename ->
        if (filename.toString().startsWith("index.", true)) {
            cache.addLinkcacheEntryFor(PageGenerator.LINKTYPE_PAGE, "index", "")
            return PageMinimalInfo(
                this,
                configuration.outPath.resolve("index.html"),
                configuration.longTitle
            )
        }

        val filenameParts = computePageInformation(configuration)

        val link = configuration.outPath.relativize(filenameParts.destinationPath.parent).toString()
        if (filenameParts.menuContainerName.isBlank()) {
            cache.addLinkcacheEntryFor(PageGenerator.LINKTYPE_PAGE, filenameParts.folderDisplayName, link)
            if (filenameParts.globalPriority > 0)
                cache.addMenuItem(
                    generatorName + "_" + filenameParts.menuContainerName,
                    filenameParts.globalPriority,
                    link,
                    filenameParts.menuItemDisplayName
                )
        } else {
            cache.addLinkcacheEntryFor(
                PageGenerator.LINKTYPE_PAGE,
                "${filenameParts.menuContainerName}--${filenameParts.folderDisplayName}",
                link
            )
            if (filenameParts.globalPriority > 0)
                cache.addMenuItemInContainerNoDupes(
                    generatorName + "_" + filenameParts.menuContainerName,
                    filenameParts.menuContainerName,
                    filenameParts.globalPriority,
                    filenameParts.menuItemDisplayName,
                    link,
                    elementIndex = filenameParts.menuItemPriority
                )
        }

        return PageMinimalInfo(
            this,
            filenameParts.destinationPath,
            filenameParts.menuItemDisplayName
        )
    }
}

private fun String.displayReplace() =
    replace(displayReplacePattern, " ")
        .replace('❔', '?')


fun Path.computePageInformation(configuration: WebsiteConfiguration): PageInformation {
    val inFilename = filenameWithoutExtension

    val match =
        filenameParser.matchEntire(inFilename) ?: throw IllegalStateException("$inFilename is not a valid filename")

    val globalPriority = match.groups["globalPriority"]?.value?.toIntOrNull() ?: 0
    val menuItemPriority = match.groups["menuContainerPriority"]?.value?.toIntOrNull() ?: 0

    val menuContainerName = match.groups["menuContainerName"]
        ?.value
        ?.displayReplace()
        ?: ""

    val rawMenuItemName = match.groups["menuItemName"]
        ?.value
        ?: throw IllegalStateException("No menu item name")

    val menuItemName = rawMenuItemName.displayReplace()


    val rawFolderName = match.groups["folderName"]?.value ?: rawMenuItemName

    val folderName = rawFolderName.displayReplace()

    val outFile = configuration.inPath.relativize(this)
        .resolveSibling("${rawFolderName.lowercase(Locale.getDefault())}/index.html")
        .let { configuration.outPath.resolve(it) }


    return PageInformation(
        menuContainerName,
        outFile,
        globalPriority,
        rawMenuItemName,
        menuItemName,
        menuItemPriority,
        rawFolderName,
        folderName
    )
}

interface IPageMininmalInfo {
    val sourcePath: SourcePath
    val targetPath: TargetPath
    val title: String
}

data class PageMinimalInfo(
    override val sourcePath: SourcePath,
    override val targetPath: TargetPath,
    override val title: String
) : IPageMininmalInfo
