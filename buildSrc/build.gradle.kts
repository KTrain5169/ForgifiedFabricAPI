plugins {
    java
    `java-base`
    `java-library`
    `kotlin-dsl`
}

repositories {
    // The org.jetbrains.kotlin.jvm plugin requires a repository
    // where to download the Kotlin compiler dependencies from.
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
    maven {
        name = "Architectury"
        url = uri("https://maven.architectury.dev/")
    }
    mavenLocal()
}

dependencies {
    implementation("dev.architectury:architectury-loom:1.6.9999")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("dev.architectury:at:1.0.1")
}