dependencies {
    api(project(":commons"))
    implementation(project(":image-access"))
    api(project(":backend:config"))
    api("org.jetbrains.kotlinx:kotlinx-html-jvm:${KotlinX.HTML_VERSION}")

    implementation("org.apache.velocity:velocity-engine-core:${Dependencies.VELOCITY_VERSION}")
    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")
}
