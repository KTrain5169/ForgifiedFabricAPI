import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.FileSystemUtil
import kotlin.io.path.deleteIfExists

val versionMc: String by rootProject
val versionForge: String by rootProject
val versionForgifiedFabricLoader: String by rootProject

val loom = extensions.getByType<LoomGradleExtensionAPI>()
val sourceSets = extensions.getByType<SourceSetContainer>()

val jar = tasks.named<Jar>("jar")
val remapJar = tasks.named<RemapJarTask>("remapJar")
val localDevJar = tasks.register("localDevJar", Jar::class.java) {
    dependsOn(jar)
    from(zipTree(jar.flatMap { it.archiveFile }))
    rename("accesstransformer_dev.cfg", "accesstransformer.cfg")
    archiveClassifier = "local"
    destinationDirectory = project.layout.buildDirectory.dir("devlibs")
    manifest.from(jar.get().manifest)
}

afterEvaluate {
    configurations.named("namedElements") {
        outgoing.artifacts.clear()
        outgoing.artifact(localDevJar) {
            builtBy(localDevJar)
        }
    }
}

remapJar {
    doLast {
        FileSystemUtil.getJarFileSystem(archiveFile.get().asFile.toPath(), false).use {
            val atPath = it.get().getPath("META-INF/accesstransformer_dev.cfg")
            atPath.deleteIfExists()
        }
    }
}

tasks.withType<AbstractRemapJarTask>().configureEach {
    remapperIsolation = false
}

//configurations.named("remappedJars") {
//    isCanBeConsumed = true
//    isCanBeResolved = false
//}
//configurations.named("testModRemappedJars") {
//    isCanBeConsumed = true
//    isCanBeResolved = false
//}

//artifacts.add("remappedJars", remapJar)

val mainSourceSet = sourceSets.getByName("main")

mainSourceSet.apply {
    java {
        srcDir("src/client/java")
    }
    resources {
        srcDir("src/client/resources")
    }
}

val testmod: SourceSet by sourceSets.creating {
    compileClasspath += mainSourceSet.compileClasspath
    runtimeClasspath += mainSourceSet.runtimeClasspath

    java {
        srcDir("src/testmodClient/java")
    }
    resources {
        srcDir("src/testmodClient/resources")
    }
}

dependencies {
    "compileOnly"("dev.su5ed.sinytra:fabric-loader:$versionForgifiedFabricLoader")

    "testmodImplementation"(mainSourceSet.output)
    "testmodImplementation"("dev.su5ed.sinytra:fabric-loader:$versionForgifiedFabricLoader")
}

loom.apply {
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName = project.extensions.getByType<BasePluginExtension>().archivesName.map { "$it-refmap.json" }
    }

    runtimeOnlyLog4j = true

    runs {
        configureEach {
            property("mixin.debug", "true")
        }

        create("gametest") {
            server()
            isIdeConfigGenerated = project.rootProject == project
            name = "Testmod Game Test Server"
            source(testmod)

            // Enable the gametest runner
            property("forge.gameTestServer", "true")
        }

        create("testmodClient") {
            client()
            isIdeConfigGenerated = project.rootProject == project
            name = "Testmod Client"
            source(testmod)
        }

        create("testmodServer") {
            server()
            isIdeConfigGenerated = project.rootProject == project
            name = "Testmod Server"
            source(testmod)
        }
    }
}
