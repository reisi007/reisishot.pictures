dependencies {
    api(project(":backend:generators:thumbnail-abstract"))
    implementation(project(":backend:root"))
    api(project(":backend:html"))
    api(project(":backend:config"))
    implementation(project(":image-access"))
}
