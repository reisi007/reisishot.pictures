dependencies {
    implementation(project(":image-access"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:${KotlinX.SERIALISATION_VERSION}")
    implementation("com.typesafe:config:${Dependencies.TYPESAFE_CONFIG_VERSION}")
    implementation("io.github.config4k:config4k:${Dependencies.CONFIG4K_VERSION}")
}
