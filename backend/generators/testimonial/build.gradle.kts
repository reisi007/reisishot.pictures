val projectTestOutput = project(":commons").testSources

dependencies {
    api(project(":backend:generators:gallery-abstract"))
    implementation(project(":commons"))
    implementation(project(":backend:root"))

    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")

    testImplementation(testFixtures(project(":backend:website-config")))
    testImplementation(projectTestOutput)
}
