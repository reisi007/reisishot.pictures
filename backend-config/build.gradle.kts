val projectTestOutput: SourceSetOutput = project(":commons").sourceSets["test"].output

dependencies {
    api(project(":image-access"))
    testApi(projectTestOutput)
}
