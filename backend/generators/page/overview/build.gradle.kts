dependencies {
    api(project(":backend:generators:gallery-abstract"))

    implementation(rootProject.libs.flexmark)

    testImplementation(testFixtures(project(":backend:html")))
}
