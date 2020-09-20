package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import kotlinx.html.InputType
import kotlinx.html.h2
import kotlinx.html.h3
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import pictures.reisishot.mise.backend.generator.multisite.ImageInfoImporter
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.pages.yamlConsumer.KeywordConsumer
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import pictures.reisishot.mise.backend.html.*
import java.nio.file.Path
import java.nio.file.Paths

object ProducionBoudoir {
    @JvmStatic
    fun main(args: Array<String>) {
        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot Boudoir",
                        longTitle = "Reisishot Boudoir - Intime Portraits für dich aus Leidenschaft",
                        websiteLocation = "https://boudoir.reisishot.pictures",
                        inPath = Paths.get("input-boudoir").toAbsolutePath(),
                        tmpPath = Paths.get("tmp-boudoir").toAbsolutePath(),
                        outPath = Paths.get("frontend-boudoir/generated").toAbsolutePath(),
                        interactiveIgnoredFiles = *arrayOf<(FileExtension) -> Boolean>(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                        cleanupGeneration = false,
                        socialMediaLinks = SocialMediaAccounts(
                                "reisishot.boudoir",
                                "reisishot_boudoir",
                                "florian@reisishot.pictures",
                                "436702017710"
                        ),
                        analyticsSiteId = "4",
                        form = { target: Path, websiteConfiguration: WebsiteConfiguration ->
                            buildForm(
                                    title = { h2 { text("Möchtest du selbst ein Boudoir Shooting haben?") } },
                                    thankYouText = { h3 { text("Vielen Dank für deine Nachricht! Ich melde mich schnellstmöglich bei dir!") } },
                                    formStructure = {
                                        FormRoot("footer",
                                                HiddenFormInput("Seite", websiteConfiguration.websiteLocation + websiteConfiguration.outPath.relativize(target.parent).toString()),
                                                FormHGroup(
                                                        FormInput("Name", "Wie heißt du?", "Dein Name", "Bitte sag mir, wie du heißt", InputType.text),
                                                        FormInput("E-Mail", "Wie lautet deine E-Mail Adresse?", "Deine E-Mail-Adresse, auf die du deine Antwort bekommst", "Ich kann dich ohne deine E-Mail Adresse nicht kontaktieren", InputType.email)
                                                ),
                                                FormInput("Betreff", "Betreff", "Thema deines Anliegens", "Der Betreff der E-Mail, die ich bekomme", InputType.text),
                                                FormTextArea("Warum", "Warum möchtest du Fotos mit mir machen? Gibt es Gründe, warum du aktuell noch überlegst?", "Ich freue mich schon deine Gedanken zu hören!", errorMessage = "Damit ich dir das bestmögliche Erlebnis anbieten kann, brauche ich diese Informationen."),
                                                FormCheckbox("Zustimmung", "Mir ist bewusst dass dieses Formular Daten an den Fotografen sendet.", "Natürlich wird diese E-Mail-Adresse nur zum Zwecke deiner Anfrage verwendet und nicht mit Dritten geteilt", "Leider benötige ich deine Einwilligung, damit du mir eine Nachricht schicken darfst")
                                        )
                                    })
                        },
                        generators = listOf(
                                PageGenerator(
                                        KeywordConsumer()
                                ),
                                GalleryGenerator(
                                        categoryBuilders = *emptyArray(),
                                        exifReplaceFunction = defaultExifReplaceFunction
                                ),
                                ImageMagickThumbnailGenerator(),
                                ImageInfoImporter(Paths.get("tmp-main").toAbsolutePath()),
                                SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                        )
                )
        )
    }
}