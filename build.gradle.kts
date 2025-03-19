plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.6"
    application
}

group = "org.nous"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// expect `./gradlew run -PmainClass=storywriter.scripts.VariatorKt` or similar on command line
application {
    val mc = project.findProperty("mainClass")?.toString()
    if(mc != null)
        mainClass.set(mc)
    else
        mainClass.set("MainKt")
}

// allow interactive input via gradlew run
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

val kotlinVersion = "2.1.0"
val ktorVersion = "3.0.3"
val logbackVersion = "1.4.14"

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.nous:kaida:0.2")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
//    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-config-yaml-jvm:$ktorVersion")

    // yaml support for kotlinx.serialization
    implementation("com.charleskorn.kaml:kaml:0.65.0")

    // fast b-tree file backed local database (for development or small deployments)
    implementation("org.mapdb:mapdb:3.1.0")
    // dynamically scan for serializable types for the websocket UserInteraction implementation
    implementation("io.github.classgraph:classgraph:4.8.179")
    // command line arguments
    implementation("com.github.ajalt.clikt:clikt:5.0.3")

    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.4.0")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveBaseName.set("storywriter.jar")
        archiveClassifier.set("")
        archiveVersion.set("")
        manifest {
            attributes["Main-Class"] = "MainKt"
        }
    }
}
