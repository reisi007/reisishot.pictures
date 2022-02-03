dependencies {
    api("org.jetbrains.kotlinx:kotlinx-html-jvm:${KotlinX.HTML_VERSION}")
    api(project(":backend:root"))
    implementation(project(":image-access"))
    api(project(":commons"))
    implementation("org.apache.velocity:velocity-engine-core:${Dependencies.VELOCITY_VERSION}")
    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")
    testImplementation(testFixtures(project(":backend:website-config")))

}
