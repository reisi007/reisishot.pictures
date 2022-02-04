val projectTestOutput = project(":commons").testSources

dependencies {
    api(project(":image-access"))
    api(project(":backend:root"))
    testApi(projectTestOutput)
}
