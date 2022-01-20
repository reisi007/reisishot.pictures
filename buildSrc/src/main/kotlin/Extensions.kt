import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceSetOutput

val Project.sourceSets: SourceSetContainer
    get() = ((this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer)

val Project.testSources: SourceSetOutput
    get() = sourceSets.getByName("test").output
