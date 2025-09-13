import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("fabric-loom") version "1.9-SNAPSHOT"
}

group = project.property("maven_group").toString()
version = project.property("mod_version").toString()

val mcVersion = project.property("mc_version").toString()
val fabricLoaderVersion = project.property("fabric_loader_version").toString()
val jarName = "${project.property("mod_name").toString()}-$mcVersion"

repositories {
    maven("https://api.modrinth.com/maven")
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:$mcVersion+build.${project.property("yarn_build")}:v2")
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fapi_version")}+$mcVersion")

    modImplementation("maven.modrinth:carpet:${project.property("carpet_version")}")

    implementation("com.fasterxml.jackson.core:jackson-core:2.16.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

    implementation("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("com.demonwav.mcdev:annotations:1.0")
}

sourceSets {
    create("testmod") {
        compileClasspath += main.get().compileClasspath + main.get().output
        runtimeClasspath += main.get().runtimeClasspath + main.get().output
    }
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("mcDep", mcVersion)
    inputs.property("fabricLoader", fabricLoaderVersion)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
            "mcDep" to mcVersion,
            "fabricLoader" to fabricLoaderVersion
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8" // allow emoji in comments :^)
}

java {
    withSourcesJar()
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
