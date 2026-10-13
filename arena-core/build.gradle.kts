plugins {
    id("com.gradleup.shadow") version "9.2.2"
}

dependencies {
    implementation(project(":arena-api"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveBaseName.set("ArenaCore")
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "com.arenashowdown.libs.hikari")
    relocate("org.sqlite", "com.arenashowdown.libs.sqlite")
    mergeServiceFiles()
}

tasks.build { dependsOn(tasks.shadowJar) }
