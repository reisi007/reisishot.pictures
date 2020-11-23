package pictures.reisishot.mise.backend.main

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import kotlinx.html.InputType
import kotlinx.html.h2
import kotlinx.html.h3
import pictures.reisishot.mise.backend.Mise
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.multisite.ImageInfoImporter
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.pages.yamlConsumer.KeywordConsumer
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import pictures.reisishot.mise.backend.html.*
import java.nio.file.Path
import java.nio.file.Paths

object Goto {
    @JvmStatic
    fun main(args: Array<String>) {
        build(args.isEmpty())
    }

    fun build(isDevMode: Boolean) {
        val folderName = "goto.reisishot.pictures"
        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot - Herzlich Willkommen",
                        longTitle = "Reisishot Goto - Übersicht über meine Projekte",
                        isDevMode = isDevMode,
                        websiteLocation = "https://$folderName",
                        inPath = Paths.get("input", folderName).toAbsolutePath(),
                        tmpPath = Paths.get("tmp", folderName).toAbsolutePath(),
                        outPath = Paths.get("upload", folderName).toAbsolutePath(),
                        interactiveIgnoredFiles = arrayOf<(FileExtension) -> Boolean>(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                        metaDataConsumers = arrayOf(KeywordConsumer()),
                        cleanupGeneration = false,
                        analyticsSiteId = "6",
                        form = { target: Path, websiteConfiguration: WebsiteConfiguration ->
                            buildForm(
                                    title = { h2 { text("Kontaktiere mich") } },
                                    thankYouText = { h3 { text("Vielen Dank für deine Nachricht! Ich melde mich innerhalb von 48h!") } },
                                    formStructure = {
                                        FormRoot("footer",
                                                HiddenFormInput("Seite", BuildingCache.getLinkFromFragment(websiteConfiguration, websiteConfiguration.outPath.relativize(target.parent).toString())),
                                                FormHGroup(
                                                        FormInput("Name", "Name", "Dein Name", "Bitte sag mir, wie du heißt", InputType.text),
                                                        FormInput("E-Mail", "E-Mail Adresse", "Deine E-Mail-Adresse, auf die du deine Antwort bekommst", "Ich kann dich ohne deine E-Mail Adresse nicht kontaktieren", InputType.email)
                                                ),
                                                FormInput("Betreff", "Betreff", "Thema deines Anliegens", "Der Betreff der E-Mail, die ich bekomme", InputType.text),
                                                FormTextArea("Freitext", "Deine Nachricht an mich", "Versuche möglichst genau zu sein, da ich dann besser auf dich eingehen kann", "Bitte vergiss nicht mir eine Nachricht zu hinterlassen"),
                                                FormCheckbox("Zustimmung", "Ich akzeptiere, dass der Inhalt dieses Formulars per Mail an den Fotografen zugestellt wird", "Natürlich wird diese E-Mail-Adresse nur zum Zwecke deiner Anfrage verwendet und nicht mit Dritten geteilt", "Leider benötige ich deine Einwilligung, damit du mir eine Nachricht schicken darfst")
                                        )
                                    })
                        },
                        generators = listOf(
                                GalleryGenerator(
                                        categoryBuilders = emptyArray(),
                                        exifReplaceFunction = defaultExifReplaceFunction
                                ),
                                ImageInfoImporter(Main.tmpPath),
                                PageGenerator(),
                                SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                        )
                )
        )
    }
}