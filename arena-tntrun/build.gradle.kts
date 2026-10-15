
plugins {
    id("com.gradleup.shadow") version "9.2.2"
}

version = "0.1.0-SNAPSHOT"

dependencies {
    compileOnly(project(":arena-api"))
    testImplementation(project(":arena-api"))
    testImplementation(project(":arena-core"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveBaseName.set("ArenaTNTRun")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Test-only plugin for validating real Paper serialization and block recovery.
// Never bundled in the production ArenaTNTRun JAR.
val paperVerification by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + configurations.compileClasspath.get()
}
val paperVerificationJar by tasks.registering(Jar::class) {
    dependsOn(paperVerification.classesTaskName)
    archiveBaseName.set("ArenaTNTRunVerification")
    from(paperVerification.output)
}
tasks.check { dependsOn(paperVerificationJar) }
