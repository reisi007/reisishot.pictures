plugins {

    id("org.openjfx.javafxplugin") version "0.0.10"
}

dependencies {
}

subprojects {
    apply(plugin = "org.openjfx.javafxplugin")

    javafx {
        val jvmTarget: String by extra
        version = jvmTarget
        modules = listOf("javafx.controls")
    }

    dependencies {
        implementation(project(":commons"))
    }
}
