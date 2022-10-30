plugins {
    id("org.jetbrains.compose") version "1.2.0"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation(project(":image-access"))
    implementation(libs.languagetool.de)
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
