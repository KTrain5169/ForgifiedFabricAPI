plugins {
    java
    `maven-publish`
    id("net.neoforged.gradleutils").version("3.0.0-alpha.10")
    id("dev.architectury.loom") version "1.6.9999"
}

val versionMc: String by rootProject
val versionForge: String by rootProject
val versionYarn: String by project

group = "dev.su5ed.sinytra"
version = "0.0.0-SNAPSHOT"

//gradleutils.version {
//    branches {
//        suffixBranch()
//        suffixExemptedBranch(versionMc)
//    }
//}
//version = "${gradleutils.version}+$versionLoaderUpstream+$versionMc"
//println("Version: $version")

val yarnMappings: Configuration by configurations.creating

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }
    maven {
        name = "Mojank"
        url = uri("https://libraries.minecraft.net/")
    }
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
}

dependencies {
    "minecraft"(group = "com.mojang", name = "minecraft", version = versionMc)
    "neoForge"(group = "net.neoforged", name = "neoforge", version = versionForge)
    "mappings"(loom.layered {
        mappings(group = "net.fabricmc", name = "yarn", version = versionYarn, classifier = "v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:1.20.5+build.3")
    })
}

tasks {
    withType<JavaCompile> {
        options.release = 21
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "Su5eD"
            url = uri("https://maven.su5ed.dev/releases")
            credentials {
                username = System.getenv("MAVEN_USER") ?: "not"
                password = System.getenv("MAVEN_PASSWORD") ?: "set"
            }
        }
    }
}

