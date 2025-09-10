import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("fabric-loom") version "1.9-SNAPSHOT"
}

group = project.property("maven_group").toString()
version = project.property("mod_version").toString()

sourceSets {
    create("testmod") {
        compileClasspath += main.get().compileClasspath + main.get().output
        runtimeClasspath += main.get().runtimeClasspath + main.get().output
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("mc_version")}")
    mappings("net.fabricmc:yarn:${project.property("mc_version")}+build.${project.property("yarn_build")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fapi_version")}+${project.property("mc_version")}")

    implementation("com.google.code.findbugs:jsr305:3.0.2")
    modImplementation("com.github.gnembon:fabric-carpet:${project.property("carpet_version")}")
    compileOnly("com.demonwav.mcdev:annotations:1.0")

    implementation("com.fasterxml.jackson.core:jackson-core:2.16.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
}

tasks.register<Jar>("testmodJar") {
    dependsOn(tasks.named("testmodClasses"))
    archiveBaseName.set("Otomaton")
    archiveClassifier.set("dev")
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.register<RemapJarTask>("remapTestmodJar") {
    dependsOn(tasks.named("testmodJar"))
    archiveBaseName.set("Otomaton")
    inputFile.set(tasks.named<Jar>("testmodJar").get().archiveFile)
    addNestedDependencies.set(false)
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8" // allow emoji in comments :^)
}

java {
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes(
                "MixinConfigs" to "mixins.automatone.json",
                "Implementation-Title" to "SecondBrainEngine",
                "Implementation-Version" to version
        )
    }
}