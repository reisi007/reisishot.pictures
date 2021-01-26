package pictures.reisishot.mise.backend.main

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import kotlinx.html.InputType
import kotlinx.html.h2
import kotlinx.html.h3
import pictures.reisishot.mise.backend.Mise
import pictures.reisishot.mise.backend.SocialMediaAccounts
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache.Companion.getLinkFromFragment
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.getOrThrow
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

object Boudoir {
    @JvmStatic
    fun main(args: Array<String>) {
        build(args.isEmpty())
    }

    fun build(isDevMode: Boolean) {
        val folderName = "boudoir.reisishot.pictures"

        val galleryGenerator = GalleryGenerator(
                categoryBuilders = emptyArray(),
                exifReplaceFunction = defaultExifReplaceFunction
        )
        val overviewPageGenerator = OverviewPageGenerator(galleryGenerator)


        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot Boudoir",
                        longTitle = "Reisishot Boudoir - Intime Porträts für dich aus Leidenschaft",
                        isDevMode = isDevMode,
                        websiteLocation = "https://$folderName",
                        inPath = Paths.get("input", folderName).toAbsolutePath(),
                        tmpPath = Paths.get("tmp", folderName).toAbsolutePath(),
                        outPath = Paths.get("upload", folderName).toAbsolutePath(),
                        interactiveIgnoredFiles = arrayOf(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                        metaDataConsumers = arrayOf(overviewPageGenerator, KeywordConsumer()),
                        cleanupGeneration = false,
                        cssFileName = "styles-boudoir.css",
                        navbarBrandFunction = { config, gallery ->
                            insertLazyPicture(gallery.cache.imageInformationData.getOrThrow("Boudoir-Logo", "Menu"), config, "solo")
                        },
                        socialMediaLinks = SocialMediaAccounts(
                                "reisishot.boudoir",
                                "reisishot_boudoir",
                                "florian@reisishot.pictures",
                                "436702017710"
                        ),
                        analyticsSiteId = "4",
                        /*  fbMessengerChatPlugin = FacebookMessengerChatPlugin(
                                  107984484236335,
                                  "#d3973b",
                                  "Hallo! Was kann ich für dich tun?"
                          ),*/
                        form = { target: Path, websiteConfiguration: WebsiteConfiguration ->
                            buildForm(
                                    title = { h2 { text("Möchtest du selbst ein Boudoir Shooting haben?") } },
                                    thankYouText = { h3 { text("Vielen Dank für deine Nachricht! Ich melde mich innerhalb von 48h bei dir!") } },
                                    formStructure = {
                                        FormRoot("footer",
                                                HiddenFormInput("Seite", getLinkFromFragment(websiteConfiguration, websiteConfiguration.outPath.relativize(target.parent).toString())),
                                                FormHGroup(
                                                        FormInput("Name", "Wie heißt du?", "Dein Name", "Bitte sag mir, wie du heißt", InputType.text),
                                                        FormInput("E-Mail", "Wie lautet deine E-Mail Adresse?", "Deine E-Mail-Adresse, auf die du deine Antwort bekommst", "Ich kann dich ohne deine E-Mail Adresse nicht kontaktieren", InputType.email)
                                                ),
                                                FormInput("Betreff", "Betreff", "Thema deines Anliegens", "Der Betreff der E-Mail, die ich bekomme", InputType.text, defaultValue = "Anfrage Boudoir Shooting"),
                                                FormTextArea("Warum", "Warum möchtest du Fotos mit mir machen? Gibt es Gründe, warum du aktuell noch überlegst?", "Ich freue mich schon deine Gedanken zu hören!", errorMessage = "Damit ich dir das bestmögliche Erlebnis anbieten kann, brauche ich diese Informationen."),
                                                zustimmung
                                        )
                                    })
                        },
                        generators = listOf(
                                PageGenerator(),
                                overviewPageGenerator,
                                galleryGenerator,
                                ImageMagickThumbnailGenerator(),
                                ImageInfoImporter(Main.tmpPath, "https://${Main.folderName}/"),
                                LinkGenerator(),
                                SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                        )
                )
        )
    }
}