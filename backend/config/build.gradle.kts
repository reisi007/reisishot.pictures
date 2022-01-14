val projectTestOutput = project(":commons").testSources

dependencies {
    implementation(project(":image-access"))
    implementation(project(":backend:root"))
    testApi(projectTestOutput)
}
