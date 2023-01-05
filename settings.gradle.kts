rootProject.name = "My website (reisishot.pictures)"

include("backend")
include("commons")
include("config-ui")
include("image-access")
include("mise-utils")
include("next-cli")

pluginManagement {
    // https://kotlinlang.org/releases.html#release-details
    val kotlinVersion by extra("1.7.20")
    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("org.sonarqube") version "3.3"
        id("org.barfuin.gradle.jacocolog") version "2.0.0"
    }

    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

enableFeaturePreview("VERSION_CATALOGS")
dependencyResolutionManagement {
    // Kotlin & KotlinX versions
    val kotlinVersion: String by extra
    val kotlinxCoroutinesVersion = "1.6.4"
    val kotlinxSerializationVersion = "1.4.1"

    // Testing library versions
    val assertKVersion = "0.25"
    val assertJVersion = "3.23.1"
    val junitVersion = "5.8.2"

    // Other libraries
    val metadataExtractor = "2.18.0"
    val languagetoolVersion = "5.9"
    val composeVersion = "1.2.0"

    versionCatalogs {
        create("libs") {
            version("kotlin", kotlinVersion)
            version("sonarqube", "3.3")
            version("jacocolog", "2.0.0")
            version("compose", "1.2.0")

            library("kotlin.stdlib", "org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion}")
            library("kotlinx.coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${kotlinxCoroutinesVersion}")
            library(
                "kotlinx.serialization.json",
                "org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlinxSerializationVersion}"
            )
            library("assertk", "com.willowtreeapps.assertk:assertk-jvm:${assertKVersion}")
            library("assertj", "org.assertj:assertj-core:${assertJVersion}")

            library("junit.api", "org.junit.jupiter:junit-jupiter-api:$junitVersion")
            library("junit.params", "org.junit.jupiter:junit-jupiter-params:$junitVersion")
            library("junit.engine", "org.junit.jupiter:junit-jupiter-engine:$junitVersion")
            library("images.metadataextractor", "com.drewnoakes:metadata-extractor:${metadataExtractor}")
            library("languagetool.de", "org.languagetool:language-de:${languagetoolVersion}")
            library(
                "compose.material.icons.extended",
                "org.jetbrains.compose.material:material-icons-extended-desktop:$composeVersion"
            )
        }
    }
}
