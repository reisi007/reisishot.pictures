import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version Kotlin.VERSION
    id("org.jetbrains.kotlin.plugin.serialization") version Kotlin.VERSION
    id("org.sonarqube") version "3.3"
    id("org.barfuin.gradle.jacocolog") version "2.0.0"
    jacoco
    `java-test-fixtures`
    id("org.jmailen.kotlinter") version "3.8.0"
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
                "sonar.coverage.jacoco.xmlReportPaths" to "$buildDir/reports/jacoco/jacocoAggregatedReport/jacocoAggregatedReport.xml",
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
    apply(plugin = "java-test-fixtures")
    apply(plugin = "org.jmailen.kotlinter")

    jacoco {
        toolVersion = "0.8.7"
    }

    kotlin.target.compilations.getByName("testFixtures") {
        associateWith(target.compilations.getByName("main"))
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
                        exclude("pictures/reisishot/velocity/**")
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

    tasks.check {
        dependsOn("formatKotlin")
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
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Kotlin.COROUTINE_VERSION}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Kotlin.SERIALISATION_VERSION}")

        testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Dependencies.ASSERTK_VERSION}")
        testImplementation("org.assertj:assertj-core:${Dependencies.ASSERTJ_VERSION}")
        testImplementation("org.junit.jupiter:junit-jupiter-api:${Dependencies.JUNIT_VERSION}")
        testImplementation("org.junit.jupiter:junit-jupiter-params:${Dependencies.JUNIT_VERSION}")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Dependencies.JUNIT_VERSION}")

        /*
        * Add commons' testFixtures to the test implementation of all projects -
        *  and add test dependencies to testFixture of this project as test dependency of all other projects
        *
        * NOTE: `:commons` should be the ONLY testFixture, which has test dependencies!
        * Therefore, this overcomplicated solution
         */
        val testFixturesImpl = ":commons"
        if (getName(this@subprojects) != testFixturesImpl) {
            testImplementation(testFixtures(project(testFixturesImpl)))
        } else {
            testFixturesImplementation("com.willowtreeapps.assertk:assertk-jvm:${Dependencies.ASSERTK_VERSION}")
            testFixturesImplementation("org.assertj:assertj-core:${Dependencies.ASSERTJ_VERSION}")
        }
    }
}

fun getName(project: Project): String = project.path
