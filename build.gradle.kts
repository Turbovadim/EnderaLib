import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "org.endera"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.exposed:exposed-core:0.44.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.0")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    implementation("mysql:mysql-connector-java:8.0.28")
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

application {
    mainClass.set("MainKt")
}