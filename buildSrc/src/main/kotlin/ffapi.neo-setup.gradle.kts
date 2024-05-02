import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.FileSystemUtil
import kotlin.io.path.deleteIfExists

val versionMc: String by rootProject
val versionForge: String by rootProject

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
    manifest.from(jar.map { it.manifest })
}

afterEvaluate {
    configurations.named("namedElements") {
        outgoing.artifacts.clear()
        outgoing.artifact(localDevJar)
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

loom.mixin {
    useLegacyMixinAp = true
    defaultRefmapName = project.extensions.getByType<BasePluginExtension>().archivesName.map { "$it-refmap.json" }
}

loom.runs {
    sourceSets.findByName("testmod")?.let { testMod ->
        create("gametest") {
            server()
            isIdeConfigGenerated = project.rootProject == project
            name = "Testmod Game Test Server"
            source(testMod)

            // Enable the gametest runner
            property("forge.gameTestServer", "true")
        }
    }
}
