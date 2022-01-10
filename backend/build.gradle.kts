dependencies {
    implementation(project(":backend-config"))
    implementation("org.apache.velocity:velocity-engine-core:${Dependencies.VELOCITY_VERSION}")
    implementation("com.vladsch.flexmark:flexmark-all:${Dependencies.FLEXMARK_VERSION}")
}
