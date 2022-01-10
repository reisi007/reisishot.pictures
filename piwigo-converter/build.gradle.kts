dependencies {
    // https://mvnrepository.com/artifact/org.jdbi/jdbi3-kotlin-sqlobject
    implementation("org.jdbi:jdbi3-kotlin-sqlobject:3.9.0")
    // https://mvnrepository.com/artifact/org.mariadb.jdbc/mariadb-java-client
    implementation("org.mariadb.jdbc:mariadb-java-client:2.4.2")
    // https://mvnrepository.com/artifact/com.beust/klaxon
    implementation("com.beust:klaxon:5.5")
    implementation(project(":commons"))
}
