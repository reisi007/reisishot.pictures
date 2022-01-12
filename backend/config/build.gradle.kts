val projectTestOutput = project(":commons").testSources

dependencies {
    implementation(project(":image-access"))
    testApi(projectTestOutput)
}
