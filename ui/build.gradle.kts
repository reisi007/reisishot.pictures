plugins {
    id("org.openjfx.javafxplugin") version JavaFx.PLUGIN_VERSION
}

dependencies {
}

subprojects {
    apply(plugin = "org.openjfx.javafxplugin")

    javafx {
        version = Java.JVM_TARGET
        modules = listOf("javafx.controls")
    }

    dependencies {
        implementation(project(":commons"))
    }
}
