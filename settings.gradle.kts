pluginManagement {
    repositories {
        maven("https://server.bbkr.space/artifactory/libs-release/") {
            name = "Cotton"
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/snapshots") {
            name = "KikuGie Snapshots"
        }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
}

stonecutter {
    create(rootProject) {
        versions("1.20.1") //"1.21.1"
        vcsVersion = "1.20.1"
    }
}

rootProject.name = "secondbrainengine"