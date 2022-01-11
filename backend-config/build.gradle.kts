val projectTestOutput = project(":commons").testSources

dependencies {
    api(project(":image-access"))
    testApi(projectTestOutput)
}
