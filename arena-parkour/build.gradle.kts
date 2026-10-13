plugins { id("com.gradleup.shadow") version "9.2.2" }

version = "0.3.0-SNAPSHOT"

dependencies {
    compileOnly(project(":arena-api"))
    testImplementation(project(":arena-api"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveBaseName.set("ArenaParkour")
    archiveClassifier.set("")
}
tasks.build { dependsOn(tasks.shadowJar) }
