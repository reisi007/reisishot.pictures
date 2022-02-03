package pictures.reisishot.mise.backend.config

import com.google.common.jimfs.Jimfs
import pictures.reisishot.mise.commons.withChild
import java.nio.file.Path

@WebsiteConfigBuilderDsl
fun buildTestWebsiteConfig(
    memoryFsRoot: Path = Jimfs.newFileSystem().getPath("root"),
    paths: PathInformation = PathInformation(
        memoryFsRoot withChild "in",
        memoryFsRoot withChild "tmp",
        memoryFsRoot withChild "out"
    ),
    generalWebsiteInformation: GeneralWebsiteInformation = GeneralWebsiteInformation(
        "Test", "Long test", "https://example.com"
    ),
    miseConfig: MiseConfig = MiseConfig(),
    configurer: WebsiteConfigBuilder.() -> Unit = {}
) = buildWebsiteConfig(
    paths,
    generalWebsiteInformation,
    miseConfig,
    configurer
)

