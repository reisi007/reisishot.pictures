import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version Kotlin.VERSION
    id("org.jetbrains.kotlin.plugin.serialization") version Kotlin.VERSION
}

repositories {
    mavenCentral()
}

subprojects {

    group = "at.reisishot.mise"
    version = "1.0-SNAPSHOT"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    val compileKotlin: KotlinCompile by tasks
    val compileTestKotlin: KotlinCompile by tasks

    compileKotlin.kotlinOptions {
        jvmTarget = Java.JVM_TARGET
    }

    compileTestKotlin.kotlinOptions {
        jvmTarget = Java.JVM_TARGET

    }

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs = Java.COMPILE_ARGS
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
