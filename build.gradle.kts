import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.JavaVersion

plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

group = project.property("maven_group").toString()
version = project.property("mod_version").toString()

val mcVersion = project.property("mc_version").toString()
val mcDep = project.property("mc_dep").toString()
val fabricLoaderVersion = project.property("fabric_loader_version").toString()
val jarName = "${project.property("mod_name").toString()}-$mcVersion"
val fapiVersion = "${project.property("fapi_version")}+$mcVersion"

val javaVersion = if (stonecutter.eval(mcVersion, ">=1.20.6")) JavaVersion.VERSION_21 else JavaVersion.VERSION_17

repositories {
    maven("https://api.modrinth.com/maven")
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:$mcVersion+build.${project.property("yarn_build")}:v2")
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fapiVersion")

    modImplementation("maven.modrinth:carpet:${project.property("carpet_version")}")

    implementation("com.fasterxml.jackson.core:jackson-core:2.18.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")

    implementation("com.google.code.findbugs:jsr305:3.0.2")
}

sourceSets {
    val testmod by creating {
        compileClasspath += main.get().compileClasspath + main.get().output
        runtimeClasspath += main.get().runtimeClasspath + main.get().output
    }
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("mcDep", mcDep)
    inputs.property("fabricLoader", fabricLoaderVersion)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
            "mcDep" to mcDep,
            "fabricLoader" to fabricLoaderVersion
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8" // allow emoji in comments :^)
}

java {
    withSourcesJar()
    targetCompatibility = javaVersion
    sourceCompatibility = javaVersion
}

tasks.remapJar {
    archiveBaseName.set(jarName)
}

tasks.remapSourcesJar {
    archiveBaseName.set(jarName)
}

tasks.register<Jar>("testmodJar") {
    dependsOn(tasks.named("testmodClasses"))
    archiveBaseName.set("otomaton")
    archiveClassifier.set("dev")
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.register<RemapJarTask>("remapTestmodJar") {
    dependsOn(tasks.named("testmodJar"))
    archiveBaseName.set("otomaton")
    inputFile.set(tasks.named<Jar>("testmodJar").get().archiveFile)
    addNestedDependencies.set(false)
}
