dependencies {
    api(project(":backend:generators:gallery-abstract"))

    implementation(project(":backend:html"))
    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")

    testImplementation(testFixtures(project(":backend:html")))
}
