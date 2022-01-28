import org.gradle.api.tasks.testing.logging.TestExceptionFormat


plugins {
    kotlin("jvm") version Kotlin.VERSION
    jacoco
    id("org.jetbrains.kotlin.plugin.serialization") version Kotlin.VERSION
    id("org.sonarqube") version "3.3"
}

repositories {
    mavenCentral()
}

sonarqube {
    properties {
        property("sonar.projectKey", "reisi007_reisishot.pictures")
        property("sonar.organization", "reisi007")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.exclusions",
            listOf(
                "**/backend/html/src/main/java/**/*" // (Once) generated / copied code
            )
        )
    }
}

subprojects {

    group = "at.reisishot.mise"
    version = "1.0-SNAPSHOT"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "jacoco")

    val compileKotlin by tasks.compileKotlin
    val compileTestKotlin by tasks.compileTestKotlin
    val compileJava by tasks.compileJava

    for (cur in listOf(compileKotlin, compileTestKotlin)) {
        cur.apply {
            sourceCompatibility = Java.JVM_TARGET
            targetCompatibility = Java.JVM_TARGET
            kotlinOptions.jvmTarget = Java.JVM_TARGET
        }
    }

    compileJava.apply {
        options.compilerArgs = Java.COMPILE_ARGS
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
        }
    }

    tasks.test {
        finalizedBy("jacocoTestReport")
    }

    java.sourceCompatibility = Java.JVM_TARGET_VERSION
    java.targetCompatibility = Java.JVM_TARGET_VERSION

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Kotlin.VERSION}")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${KotlinX.COROUTINE_VERSION}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${KotlinX.SERIALISATION_VERSION}")

        testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Dependencies.ASSERTK_VERSION}")
        testImplementation("org.assertj:assertj-core:${Dependencies.ASSERTJ_VERSION}")
        testImplementation("org.junit.jupiter:junit-jupiter-api:${Dependencies.JUNIT_VERSION}")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Dependencies.JUNIT_VERSION}")
    }

    tasks.test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}
