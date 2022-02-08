dependencies {
    api(project(":backend:generators:gallery-abstract"))

    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")

    testImplementation(testFixtures(project(":backend:website-config")))
}
