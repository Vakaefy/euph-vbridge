pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()

        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://repo.plasmoverse.com/releases")
        maven("https://repo.plasmoverse.com/snapshots")
        maven("https://jitpack.io")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.architectury.dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "Euph-VBridge"
