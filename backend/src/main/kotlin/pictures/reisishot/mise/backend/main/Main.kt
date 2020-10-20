package pictures.reisishot.mise.backend.main

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import kotlinx.html.InputType
import kotlinx.html.h2
import kotlinx.html.h3
import pictures.reisishot.mise.backend.Mise
import pictures.reisishot.mise.backend.SocialMediaAccounts
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.categories.ConfigurableCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.categories.DateCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import pictures.reisishot.mise.backend.generator.links.LinkGenerator
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.pages.yamlConsumer.OverviewPageGenerator
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import pictures.reisishot.mise.backend.html.*
import java.nio.file.Path
import java.nio.file.Paths

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        build(true)
    }

    fun build(isDevMode: Boolean) {
        val folderName = "reisishot.pictures"
        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot",
                        longTitle = "Reisishot - Fotograf Florian Reisinger",
                        isDevMode = isDevMode,
                        websiteLocation = "https://$folderName",
                        inPath = Paths.get("input", folderName).toAbsolutePath(),
                        tmpPath = Paths.get("tmp", folderName).toAbsolutePath(),
                        outPath = Paths.get("frontend", folderName).toAbsolutePath(),
                        interactiveIgnoredFiles = arrayOf<(FileExtension) -> Boolean>(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                        cleanupGeneration = false,
                        analyticsSiteId = "1",
                        socialMediaLinks = SocialMediaAccounts("reisishot", "reisishot", "florian@reisishot.pictures"),
                        form = { target: Path, websiteConfiguration: WebsiteConfiguration ->
                            buildForm(
                                    title = { h2 { text("Kontaktiere mich") } },
                                    thankYouText = { h3 { text("Vielen Dank für deine Nachricht! Ich melde mich schnellstmöglich!") } },
                                    formStructure = {
                                        FormRoot("footer",
                                                HiddenFormInput("Seite", websiteConfiguration.websiteLocation + websiteConfiguration.outPath.relativize(target.parent).toString()),
                                                FormHGroup(
                                                        FormInput("Name", "Name", "Dein Name", "Bitte sag mir, wie du heißt", InputType.text),
                                                        FormInput("E-Mail", "E-Mail Adresse", "Deine E-Mail-Adresse, auf die du deine Antwort bekommst", "Ich kann dich ohne deine E-Mail Adresse nicht kontaktieren", InputType.email)
                                                ),
                                                FormInput("Betreff", "Betreff", "Thema deines Anliegens", "Der Betreff der E-Mail, die ich bekomme", InputType.text),
                                                FormTextArea("Freitext", "Deine Nachricht an mich", "Anfragen für Zusammenarbeit (Bitte gib auch einen Link zu deinen Bildern in die Nachricht dazu :D), Feedback zu meinen Bildern oder was dir sonst so am Herzen liegt", "Bitte vergiss nicht mir eine Nachricht zu hinterlassen"),
                                                FormCheckbox("Zustimmung", "Ich akzeptiere, dass der Inhalt dieses Formulars per Mail an den Fotografen zugestellt wird", "Natürlich wird diese E-Mail-Adresse nur zum Zwecke deiner Anfrage verwendet und nicht mit Dritten geteilt", "Leider benötige ich deine Einwilligung, damit du mir eine Nachricht schicken darfst")
                                        )
                                    })
                        },
                        generators = listOf(
                                PageGenerator(
                                        metaDataConsumers = arrayOf(
                                                OverviewPageGenerator()
                                        )
                                ),
                                GalleryGenerator(
                                        categoryBuilders = arrayOf(
                                                DateCategoryBuilder("Chronologisch"),
                                                ConfigurableCategoryBuilder()
                                        ),
                                        displayedMenuItems = emptySet(),
                                        exifReplaceFunction = defaultExifReplaceFunction
                                ),
                                ImageMagickThumbnailGenerator(),
                                LinkGenerator(),
                                SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                        )
                )
        )
    }
}