plugins {
    id("org.jetbrains.compose") version "1.3.0-rc01"
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
    // https://mvnrepository.com/artifact/androidx.compose.material/material-icons-extended
    implementation(libs.compose.material.icons.extended)

}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
