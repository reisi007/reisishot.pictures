val libs = rootProject.libs
dependencies {
    api(libs.kotlinx.html)
    api(project(":backend:root"))
    implementation(libs.apache.velocity.engine)
    implementation(libs.flexmark)
    testImplementation(testFixtures(project(":backend:website-config")))
}
