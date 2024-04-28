import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

group = "org.endera"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val deps = dependencies {
    // Minecraft APIs

    compileOnly("net.kyori:adventure-text-minimessage:4.16.0")

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // Exposed
    api("org.jetbrains.exposed:exposed-core:0.49.0")
    api("org.jetbrains.exposed:exposed-dao:0.49.0")
    api("org.jetbrains.exposed:exposed-jdbc:0.49.0")

    api("com.zaxxer:HikariCP:5.1.0")

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
    api("com.charleskorn.kaml:kaml:0.59.0")

    // Database drivers
    runtimeOnly("com.mysql:mysql-connector-j:8.3.0")
    runtimeOnly("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.3.1")
    runtimeOnly("com.h2database:h2:2.2.224")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.endera.enderalib"
            artifactId = "enderalib"
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}


tasks.processResources {
    dependsOn(generatePluginYml)
}

tasks.shadowJar {
    archiveClassifier.set("shaded")
}

val generatePluginYml = tasks.create("generatePluginYml", Copy::class.java) {
    from("/src/main/templates/plugin.yml")
    into("/src/main/resources")
    expand(mapOf("projectVersion" to project.version))
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