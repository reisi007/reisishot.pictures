package pictures.reisishot.mise.backend.html.config

import kotlinx.html.DIV
import kotlinx.html.HtmlBlockTag
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteConfigBuilder
import pictures.reisishot.mise.backend.config.WebsiteConfigBuilderDsl
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata
import java.nio.file.Path

private const val CONFIG_KEY = "HTML_CONFIG"

data class HtmlConfig(
    val matomoId: Int?,
    val socialMediaLinks: SocialMediaAccounts?,
    val fbMessengerChatPlugin: FacebookMessengerChatPlugin?,
    val navbarBrandCreator: (HtmlBlockTag.(WebsiteConfig) -> Unit)?,
    val bootsrapMenuBreakpoint: String,
    val templateObjects: Map<String, VelocityTemplateObjectCreator>,
    val formBuilder: FormBuilder?
)

data class SocialMediaAccounts(
    val facebook: String? = null,
    val instagram: String? = null,
    val mail: String? = null,
    val whatsapp: String? = null,
    val podcast: String? = null
)

data class FacebookMessengerChatPlugin(
    val pageId: Long,
    val themeColor: String,
    val message: String
)

@WebsiteConfigBuilderDsl
fun WebsiteConfigBuilder.buildHtmlConfig(
    matomoId: Int? = null,
    socialMediaLinks: SocialMediaAccounts? = null,
    fbMessengerChatPlugin: FacebookMessengerChatPlugin? = null,
    formBuilder: FormBuilder? = null,
    navbarBrandCreator: (HtmlBlockTag.(WebsiteConfig) -> Unit)? = null,
    bootsrapMenuBreakpoint: String = "md",
    action: VelocityObjectCreatorMutableMap.() -> Unit
) {
    val templateObjects = mutableMapOf<String, VelocityTemplateObjectCreator>()
        .apply(action)
        .toMap()

    additionalConfig[CONFIG_KEY] =
        HtmlConfig(
            matomoId,
            socialMediaLinks,
            fbMessengerChatPlugin,
            navbarBrandCreator,
            bootsrapMenuBreakpoint,
            templateObjects,
            formBuilder
        )
}

val WebsiteConfig.htmlConfig
    get() = additionalConfig[CONFIG_KEY] as HtmlConfig

interface TemplateObject
typealias FormBuilder = DIV.(Path, WebsiteConfig) -> Unit
typealias VelocityTemplateObjectCreator = (PageMetadata?, WebsiteConfig, BuildingCache) -> TemplateObject
typealias VelocityObjectCreatorMutableMap = MutableMap<String, VelocityTemplateObjectCreator>

@WebsiteConfigBuilderDsl
fun VelocityObjectCreatorMutableMap.registerAllTemplateObjects(vararg elements: Pair<String, VelocityTemplateObjectCreator>) {
    elements.forEach {
        val returnValue = put(it.first, it.second)
        if (returnValue != null)
            error("Multiple template objects for key ${it.first} found!")
    }
}
