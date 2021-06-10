package pictures.reisishot.mise.backend.main

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import kotlinx.html.InputType
import kotlinx.html.h2
import kotlinx.html.h3
import pictures.reisishot.mise.backend.Mise
import pictures.reisishot.mise.backend.SocialMediaAccounts
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.categories.ConfigurableCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.categories.DateCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import pictures.reisishot.mise.backend.generator.links.LinkGenerator
import pictures.reisishot.mise.backend.generator.multisite.ImageInfoImporter
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.pages.overview.OverviewPageGenerator
import pictures.reisishot.mise.backend.generator.pages.yamlConsumer.KeywordConsumer
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import pictures.reisishot.mise.backend.html.*
import java.nio.file.Path
import java.nio.file.Paths

object Couples {
    private const val folderName = "couples.reisishot.pictures"
    val tmpPath: Path = Paths.get("tmp", folderName).toAbsolutePath()

    @JvmStatic
    fun main(args: Array<String>) {
        build(args.isEmpty())
    }

    fun build(isDevMode: Boolean) {
        val galleryGenerator = GalleryGenerator(
            categoryBuilders = arrayOf(
                DateCategoryBuilder("Chronologisch"),
                ConfigurableCategoryBuilder()
            ),
            displayedMenuItems = emptySet(),
            exifReplaceFunction = defaultExifReplaceFunction
        )

        val overviewPageGenerator = OverviewPageGenerator(galleryGenerator)
        Mise.build(
            WebsiteConfiguration(
                shortTitle = "Reisishot Couples",
                longTitle = "Reisishot Couples - Fotograf Florian Reisinger",
                isDevMode = isDevMode,
                websiteLocation = "https://$folderName",
                inPath = Paths.get("input", folderName).toAbsolutePath(),
                tmpPath = tmpPath,
                outPath = Paths.get("upload", folderName).toAbsolutePath(),
                interactiveIgnoredFiles = arrayOf(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                cleanupGeneration = false,
                analyticsSiteId = "5",
                socialMediaLinks = SocialMediaAccounts("reisishot", "reisishot", "florian@reisishot.pictures"),
                // fbMessengerChatPlugin = generateDefaultChatPlugin(),
                form = { target: Path, websiteConfiguration: WebsiteConfiguration ->
                    buildForm(
                        title = { h2 { text("Kontaktiere mich") } },
                        thankYouText = { h3 { text("Vielen Dank für deine Nachricht! Ich melde mich innerhalb von 48h!") } },
                        formStructure = {
                            FormRoot(
                                "footer",
                                HiddenFormInput(
                                    "Seite",
                                    BuildingCache.getLinkFromFragment(
                                        websiteConfiguration,
                                        websiteConfiguration.outPath.relativize(target.parent).toString()
                                    )
                                ),
                                FormHGroup(
                                    FormInput(
                                        "Name",
                                        "Name",
                                        "Dein Name",
                                        "Bitte sag mir, wie du heißt",
                                        InputType.text
                                    ),
                                    FormInput(
                                        "E-Mail",
                                        "E-Mail Adresse",
                                        "Deine E-Mail-Adresse, auf die du deine Antwort bekommst",
                                        "Ich kann dich ohne deine E-Mail Adresse nicht kontaktieren",
                                        InputType.email
                                    )
                                ),
                                FormInput(
                                    "Betreff",
                                    "Betreff",
                                    "Thema deines Anliegens",
                                    "Der Betreff der E-Mail, die ich bekomme",
                                    InputType.text
                                ),
                                FormTextArea(
                                    "Freitext",
                                    "Deine Nachricht an mich",
                                    "Anfragen für Zusammenarbeit (Bitte gib auch einen Link zu deinen Bildern in die Nachricht dazu), Feedback zu meinen Bildern oder was dir sonst so am Herzen liegt",
                                    "Bitte vergiss nicht mir eine Nachricht zu hinterlassen"
                                ),
                                zustimmung
                            )
                        })
                },
                generators = listOf(
                    PageGenerator(overviewPageGenerator, KeywordConsumer()),
                    ImageInfoImporter(Main.tmpPath, "https://${Main.folderName}/"),
                    overviewPageGenerator,
                    galleryGenerator,
                    ImageMagickThumbnailGenerator(),
                    LinkGenerator(),
                    SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                )
            )
        )
    }
}
