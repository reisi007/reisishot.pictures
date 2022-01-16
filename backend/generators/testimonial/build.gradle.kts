dependencies {
    implementation(project(":commons"))
    implementation(project(":backend:root"))
    api(project(":backend:generators:gallery-abstract"))
    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")
}
