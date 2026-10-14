
plugins {
    id("com.gradleup.shadow") version "9.2.2"
}

version = "0.1.0-SNAPSHOT"

dependencies {
    compileOnly(project(":arena-api"))
    testImplementation(project(":arena-api"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveBaseName.set("ArenaTNTRun")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
