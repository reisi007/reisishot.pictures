rootProject.name = "My website (reisishot.pictures)"
// Utils
include("commons")
include("image-access")
// Split up backend dependencies
include("backend:gallery-config")
include("backend:website-config")
include("backend:html")
include("backend:root")
include("backend:generators:gallery-abstract")
include("backend:generators:page")
include("backend:generators:page:keyword")
include("backend:generators:page:minimal")
include("backend:generators:page:overview")
include("backend:generators:link")
include("backend:generators:testimonial")
include("backend:generators:sitemap")
include("backend:generators:gallery")
include("backend:generators:thumbnail-abstract")
include("backend:generators:thumbnail-imagick")
include("backend:generators:multisite")
// Projects (depend on utils but not on each other)
include("backend")
include("backend:runner")
include("mise-utils")
// Meta Subproject for all uis
include("config-ui")
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
    val kotlinxCoroutinesVersion = "1.6.2"
    val kotlinxSerializationVersion = "1.3.2"
    val kotlinxHtmlVersion = "0.7.5"

    // Testing library versions
    val assertKVersion = "0.25"
    val assertJVersion = "3.23.1"
    val junitVersion = "5.8.2"

    // Other libraries
    val metadataExtractor = "2.18.0"
    val velocityVersion = "2.3"
    val flexmarkVersion = "0.64.0"
    val jimFsVersion = "1.2"
    val tornadoFxVersion = "1.7.20"
    val languagetoolVersion = "5.7"
    val composeVersion = "1.2.0"

    versionCatalogs {
        create("libs") {
            version("kotlin", kotlinVersion)
            version("sonarqube", "3.3")
            version("jacocolog", "2.0.0")
            version("compose", "1.2.0")

            library("kotlin.stdlib", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
            library("kotlinx.coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${kotlinxCoroutinesVersion}")
            library("kotlinx.html", "org.jetbrains.kotlinx:kotlinx-html-jvm:${kotlinxHtmlVersion}")
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
            library("apache.velocity.engine", "org.apache.velocity:velocity-engine-core:${velocityVersion}")
            library("flexmark", "com.vladsch.flexmark:flexmark-all:${flexmarkVersion}")
            library("google.jimfs", "com.google.jimfs:jimfs:${jimFsVersion}")
            library("javafx.tornadofx", "no.tornado:tornadofx:${tornadoFxVersion}")
            library("languagetool.de", "org.languagetool:language-de:${languagetoolVersion}")
            library(
                "compose.material.icons.extended",
                "org.jetbrains.compose.material:material-icons-extended-desktop:$composeVersion"
            )
        }
    }
}
