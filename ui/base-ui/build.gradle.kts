dependencies {
    val libs = rootProject.libs
    api(project(":image-access"))
    api(libs.javafx.tornadofx)
    api(libs.languagetool.de)
}
