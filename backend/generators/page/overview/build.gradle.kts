dependencies {
    implementation(project(":backend:html"))
    api(project(":backend:generators:gallery-abstract"))
    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")
}
