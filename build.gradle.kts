import net.fabricmc.loom.build.nesting.IncludedJarFactory
import net.fabricmc.loom.build.nesting.IncludedJarFactory.LazyNestedFile
import net.fabricmc.loom.util.GroovyXmlUtil

plugins {
    java
    `maven-publish`
    id("net.neoforged.gradleutils").version("3.0.0-alpha.10")
    id("dev.architectury.loom") // Version declared in buildSrc
}

val versionMc: String by rootProject
val versionForge: String by rootProject
val versionYarn: String by project
val versionForgifiedFabricLoader: String by project
val versionFabricLoader: String by project

val META_PROJECTS: List<String> = listOf(
    "deprecated",
    "fabric-api-bom",
    "fabric-api-catalog"
)
val DEV_ONLY_MODULES: List<String> = listOf(
    "fabric-gametest-api-v1"
)

val javadocDeps: Configuration by configurations.creating

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

allprojects {
    apply(plugin = "dev.architectury.loom")

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
        maven {
            name = "Sinytra"
            url = uri("https://maven.su5ed.dev/releases")
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
}

dependencies {
    javadocDeps("net.fabricmc:fabric-loader:$versionFabricLoader")

    // Include Forgified Fabric Loader
    include("dev.su5ed.sinytra:fabric-loader:$versionForgifiedFabricLoader:full")
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

// Subprojects
subprojects {
    apply(plugin = "ffapi.neo-conversion")
    apply(plugin = "ffapi.neo-setup")

    if (!META_PROJECTS.contains(name)) {
        apply(plugin = "ffapi.neo-compat")
    }

    repositories {
        maven("https://maven.su5ed.dev/releases")
    }
}

val includedRemappedJars: Configuration by configurations.creating
val includedTestModRemappedJars: Configuration by configurations.creating

subprojects {
    if (name !in DEV_ONLY_MODULES && !META_PROJECTS.contains(name)) {
        // Include the signed or none signed jar from the sub project.
        dependencies {
//            includedRemappedJars(project(path, "remappedJars")) TODO
        }
    }

    if (!META_PROJECTS.contains(name) && (file("src/testmod").exists() || file("src/testmodClient").exists())) {
        dependencies {
//            includedTestModRemappedJars(project(path, "testModRemappedJars")) TODO
        }
    }
}

val includedJarFactory = IncludedJarFactory(project)
tasks {
    remapJar {
        forgeNestedJars.addAll(includedJarFactory.getForgeNestedJars(configurations.getByName("includedRemappedJars"))
            .map { it.left().map(LazyNestedFile::resolve) })
    }

    // TODO
//    remapTestmodJar {
//    	mustRunAfter remapJar
//    	forgeNestedJars.addAll includedJarFactory.getForgeNestedJars(configurations.includedTestModRemappedJars)
//    		.map { it.left().collect { it.resolve() } }
//    	addNestedDependencies = true
//    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("mavenJava") {
                pom.withXml {
                    val depsNode = GroovyXmlUtil.getOrCreateNode(asNode(), "dependencies")
                    rootProject.configurations.include.get().dependencies.forEach {
                        val depNode = depsNode.appendNode("dependency")
                        depNode.appendNode("groupId", it.group)
                        depNode.appendNode("artifactId", it.name)
                        depNode.appendNode("version", it.version)
                        depNode.appendNode("scope", "compile")
                    }
                }
            }
        }
    }
}
