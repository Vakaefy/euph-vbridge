import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
    kotlin("jvm") version "2.1.0"
    id("su.plo.voice.plugin.entrypoints") version "1.0.9"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

val plasmoVoiceVersion = providers.gradleProperty("plasmoVoiceVersion").get()

repositories {
    mavenCentral()

    maven("https://repo.plasmoverse.com/releases")
    maven("https://repo.plasmoverse.com/snapshots")
    maven("https://repo.essential.gg/repository/maven-public")
}

dependencies {
    compileOnly("su.plo.voice.api:client:$plasmoVoiceVersion")
    compileOnly("org.jetbrains:annotations:23.0.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("fabric.mod.json") {
        filter { line -> line.replace("\"name\": \"euph-vbridge\"", "\"name\": \"Euph-VBridge\"") }
    }
    filesMatching("META-INF/mods.toml") {
        filter { line -> line.replace("displayName = \"euph-vbridge\"", "displayName = \"Euph-VBridge\"") }
    }
    filesMatching("META-INF/neoforge.mods.toml") {
        filter { line -> line.replace("displayName = \"euph-vbridge\"", "displayName = \"Euph-VBridge\"") }
    }
    filesMatching("pack.mcmeta") {
        filter { line -> line.replace("euph-vbridge generated resources", "Euph-VBridge generated resources") }
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Jar>("shadowJar") {
    archiveBaseName.set("euph-vbridge")
    archiveClassifier.set("")
}
