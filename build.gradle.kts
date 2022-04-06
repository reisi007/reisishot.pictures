import org.gradle.api.tasks.testing.logging.TestExceptionFormat


plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin
    id("org.sonarqube") version libs.versions.sonarqube
    id("org.barfuin.gradle.jacocolog") version libs.versions.jacocolog
    jacoco
    `java-test-fixtures`
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

    val compileKotlin by tasks.compileKotlin
    val compileTestKotlin by tasks.compileTestKotlin
    val compileJava by tasks.compileJava
    val javaVersion: JavaVersion by extra(JavaVersion.valueOf(extra.get("java.version") as String))
    val jvmTarget: String by extra(javaVersion.toString())

    for (cur in listOf(compileKotlin, compileTestKotlin)) {
        cur.apply {
            sourceCompatibility = jvmTarget
            targetCompatibility = jvmTarget
            kotlinOptions.jvmTarget = jvmTarget
        }
    }

    compileJava.apply {
        options.compilerArgs = listOf(
            "-g",
            "--add-exports", "java.base/java.util=ALL-UNNAMED"
        )
        options.isDebug = true
    }

    java.sourceCompatibility = javaVersion
    java.targetCompatibility = javaVersion

    repositories {
        mavenCentral()
    }

    val libs = rootProject.libs
    dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.serialization.json)

        testImplementation(libs.assertk)
        testImplementation(libs.assertj)
        testImplementation(libs.junit.api)
        testImplementation(libs.junit.params)
        testRuntimeOnly(libs.junit.engine)


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
            testFixturesImplementation(libs.assertk)
            testFixturesImplementation(libs.assertj)
        }
    }
}

fun getName(project: Project): String = project.path
