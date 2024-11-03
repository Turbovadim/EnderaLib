import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.3"
    `maven-publish`
}

group = "org.endera"
version = "1.3.0"

repositories {
    mavenCentral()
//    maven("https://repo.papermc.io/repository/maven-public/")
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

val deps = dependencies {
    // Minecraft APIs
    val exposedVersion = "0.55.0"
    val ktorVersion = "3.0.0"

    compileOnly("net.kyori:adventure-text-minimessage:4.16.0")

    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")
//    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // Exposed
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-okhttp:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    api("com.zaxxer:HikariCP:5.1.0")

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    api("com.charleskorn.kaml:kaml:0.61.0")

    // Database drivers
    runtimeOnly("com.mysql:mysql-connector-j:9.1.0")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    runtimeOnly("com.h2database:h2:2.3.232")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.endera.enderalib"
            artifactId = "enderalib"
            version = version

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
    outputs.upToDateWhen { false }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_1_7)
    }
}
dependencies {
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.12")
}

tasks.withType<JavaCompile> {
    targetCompatibility = "17"
}