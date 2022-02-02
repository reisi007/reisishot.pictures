import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version Kotlin.VERSION
    id("org.jetbrains.kotlin.plugin.serialization") version Kotlin.VERSION
    id("org.sonarqube") version "3.3"
    id("org.barfuin.gradle.jacocolog") version "2.0.0"
    jacoco
}

repositories {
    mavenCentral()
}

sonarqube {
    properties {
        properties(
            mapOf<String, Any>(
                "sonar.projectKey" to "reisi007_reisishot.pictures",
                "sonar.organization" to "reisi007",
                "sonar.host.url" to "https://sonarcloud.io",
                "sonar.exclusions" to "**/backend/html/src/main/java/**/*",
                "sonar.coverage.jacoco.xmlReportPaths" to "${buildDir}/reports/jacoco/jacocoAggregatedReport/jacocoAggregatedReport.xml",
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

    jacoco {
        toolVersion = "0.8.7"
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude("at/reisishot/velocity/**")
                        exclude("com/vladsch/**")
                    }
                }
            )
        )
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
        finalizedBy(tasks.jacocoTestReport)
    }

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
        options.isDebug = true
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
        testImplementation("org.junit.jupiter:junit-jupiter-params:${Dependencies.JUNIT_VERSION}")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Dependencies.JUNIT_VERSION}")
    }
}
