import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSetContainer

val Project.sourceSets
    get() = ((this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer)

val Project.testSources
    get() = sourceSets.getByName("test").output
