import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.endera"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Minecraft APIs
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.48.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.48.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.48.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
    implementation("com.charleskorn.kaml:kaml:0.55.0")

    // Database drivers
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.1")

}

tasks.processResources {
    dependsOn(generatePluginYml)
}


val generatePluginYml = tasks.create("generatePluginYml", Copy::class.java) {
    from("/src/main/templates/plugin.yml")
    into("/src/main/resources")
    expand(mapOf("projectVersion" to project.version))
}

tasks.shadowJar {
    archiveClassifier.set("shaded")
}

tasks.jar {
    dependsOn(generatePluginYml)
    dependsOn("shadowJar")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaCompile> {
    targetCompatibility = "17"
}