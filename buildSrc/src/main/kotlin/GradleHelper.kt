@file:Suppress("MemberVisibilityCanBePrivate")

import org.gradle.api.JavaVersion

object Kotlin {
    // https://kotlinlang.org/releases.html#release-details
    const val VERSION = "1.6.20"
    const val SERIALISATION_VERSION = "1.3.2"
    const val COROUTINE_VERSION = "1.6.0"
    const val HTML_VERSION = "0.7.3"

}

object Java {
    val JVM_TARGET_VERSION = JavaVersion.VERSION_17
    val JVM_TARGET = JVM_TARGET_VERSION.toString()
    val COMPILE_ARGS = listOf(
        "-g",
        "--add-exports", "java.base/java.util=ALL-UNNAMED"
    )
}

object JavaFx {
    const val PLUGIN_VERSION = "0.0.10"
}

object Dependencies {
    const val ASSERTK_VERSION = "0.25"
    const val ASSERTJ_VERSION = "3.22.0"
    const val JUNIT_VERSION = "5.8.2"
    const val VELOCITY_VERSION = "2.3"
    const val FLEXMARK_VERSION = "0.62.2"
    const val METADATA_EXTRACTOR_VERSION = "2.16.0"
    const val TORNADOFX_VERSION = "1.7.20"
    const val LANGUAGETOOL_VERSION = "5.6"
    const val JIM_FS_VERSION = "1.2"
}
