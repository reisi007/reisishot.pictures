package pictures.reisishot.mise.backend.main

import kotlinx.coroutines.runBlocking
import kotlinx.html.InputType
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import pictures.reisishot.mise.backend.Mise.generate
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.GeneralWebsiteInformation
import pictures.reisishot.mise.backend.config.MiseConfig
import pictures.reisishot.mise.backend.config.PathInformation
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.buildWebsiteConfig
import pictures.reisishot.mise.backend.config.configureJsonParser
import pictures.reisishot.mise.backend.gallery.generator.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.context.createCategoryApi
import pictures.reisishot.mise.backend.generator.gallery.context.createPictureApi
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import pictures.reisishot.mise.backend.generator.links.LinkGenerator
import pictures.reisishot.mise.backend.generator.multisite.ExternalImageInformation
import pictures.reisishot.mise.backend.generator.multisite.ImageInfoImporter
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.pages.htmlparsing.context.createHtmlApi
import pictures.reisishot.mise.backend.generator.pages.minimalistic.MinimalisticPageGenerator
import pictures.reisishot.mise.backend.generator.pages.overview.OverviewPageGenerator
import pictures.reisishot.mise.backend.generator.pages.yamlConsumer.KeywordConsumer
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import pictures.reisishot.mise.backend.generator.testimonials.TestimonialLoaderImpl
import pictures.reisishot.mise.backend.generator.testimonials.createTestimonialApi
import pictures.reisishot.mise.backend.html.FormHGroup
import pictures.reisishot.mise.backend.html.FormHtml
import pictures.reisishot.mise.backend.html.FormInput
import pictures.reisishot.mise.backend.html.FormRoot
import pictures.reisishot.mise.backend.html.FormTextArea
import pictures.reisishot.mise.backend.html.HiddenFormInput
import pictures.reisishot.mise.backend.html.buildForm
import pictures.reisishot.mise.backend.html.config.SocialMediaAccounts
import pictures.reisishot.mise.backend.html.config.buildHtmlConfig
import pictures.reisishot.mise.backend.html.config.registerAllTemplateObjects
import pictures.reisishot.mise.backend.html.insertWartelisteInfo
import pictures.reisishot.mise.commons.FileExtension
import pictures.reisishot.mise.commons.isHtml
import pictures.reisishot.mise.commons.isMarkdown
import pictures.reisishot.mise.exifdata.defaultExifReplaceFunction
import pictures.reisishot.mise.private.PrivateConfig
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import pictures.reisishot.mise.backend.config.ImageInformation as ConfigImageInformation

@Suppress("MemberVisibilityCanBePrivate")
object Main {
    const val folderName = "reisinger.pictures"

    @JvmStatic
    fun main(args: Array<String>) {
        build(!args.contains("prod"))
    }

    internal fun build(isDevMode: Boolean) {

        val sourceFolder = Paths.get("input", folderName).toAbsolutePath()

        val testimonialLoader = TestimonialLoaderImpl.fromSourceFolder(sourceFolder)

        val galleryGenerator = GalleryGenerator(
            PrivateConfig.TAG_CONFIG,
            PrivateConfig.CATEGORY_CONFIG,
            emptySet(),
            defaultExifReplaceFunction
        )

        val overviewPageGenerator = OverviewPageGenerator(
            galleryGenerator,
        )

        val generators = listOf(
            PageGenerator(
                overviewPageGenerator,
                MinimalisticPageGenerator(),
                KeywordConsumer()
            ),
            overviewPageGenerator,
            testimonialLoader,
            galleryGenerator,
            ImageMagickThumbnailGenerator(),
            ImageInfoImporter(Images.cacheFolder, "https://" + Images.folderName + "/"),
            LinkGenerator(),
            SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
        )

        val websiteConfig = buildWebsiteConfig(
            PathInformation(
                sourceFolder,
                Paths.get("tmp", folderName).toAbsolutePath(),
                Paths.get("upload", folderName).toAbsolutePath()
            ),
            GeneralWebsiteInformation(
                "Reisinger - Linz",
                "Fotograf Florian Reisinger - Linz, Österreich",
                "https://$folderName",
                Locale.GERMANY,

                ),
            MiseConfig(isDevMode)
        ) {
            this.generators += generators

            configureJsonParser {
                polymorphic(ConfigImageInformation::class) {
                    subclass(InternalImageInformation::class)
                }
                polymorphic(ImageInformation::class) {
                    subclass(InternalImageInformation::class)
                    subclass(ExternalImageInformation::class)
                }
            }

            buildHtmlConfig(
                1,
                SocialMediaAccounts(
                    "reisishot",
                    "florian.reisinger.photography",
                    "florian@reisinger.pictures",
                    "436702017710"
                ),
                formBuilder = { target: Path, websiteConfig: WebsiteConfig ->
                    buildForm(
                        title = { h2 { text("Kontaktiere mich") } },
                        thankYouText = { h3 { text("Vielen Dank für deine Nachricht! Ich melde mich innerhalb von 48h!") } },
                        formStructure = {
                            FormRoot(
                                "footer",
                                HiddenFormInput(
                                    "Seite",
                                    BuildingCache.getLinkFromFragment(
                                        websiteConfig,
                                        websiteConfig.paths.targetFolder.relativize(target.parent).toString()
                                    )
                                ),
                                FormHtml { insertWartelisteInfo() },
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
                                    ),
                                    FormInput(
                                        "Telefonnummer",
                                        "Wie kann ich dich telefonisch erreichen?",
                                        "Bitte schicke mir deine Telefonnummer, falls du ein Shooting mit mir möchtest",
                                        "Bitte gib deine Telefonnummer ein",
                                        type = InputType.tel,
                                        required = false
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
                        }
                    )
                }
            ) {
                registerAllTemplateObjects(
                    createHtmlApi(),
                    galleryGenerator.createPictureApi(),
                    galleryGenerator.createCategoryApi(),
                    testimonialLoader.createTestimonialApi(galleryGenerator)
                )
            }
        }

        runBlocking { websiteConfig.generate() }
    }
}
