plugins {
    java
    id("dev.architectury.loom")
}

val versionMc: String by project
val versionForge: String by project
val versionYarn: String by project

repositories {
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = versionMc)
    neoForge(group = "net.neoforged", name = "neoforge", version = versionForge)
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:$versionYarn:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:1.20.5+build.3")
    })
}
