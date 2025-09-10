/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */
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
        versions("1.20.1", "1.21.1")
        vcsVersion = "1.20.1"
    }
}

rootProject.name = "secondbrainengine"