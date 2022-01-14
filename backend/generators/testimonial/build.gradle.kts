dependencies {
    implementation(project(":commons"))
    implementation(project(":backend:root"))
    implementation(project(":backend:html"))
    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")
}
