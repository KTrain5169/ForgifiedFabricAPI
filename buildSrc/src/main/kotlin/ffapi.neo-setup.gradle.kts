import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.FileSystemUtil
import kotlin.io.path.deleteIfExists

val versionMc: String by rootProject
val versionForge: String by rootProject
val versionForgifiedFabricLoader: String by rootProject
val versionFabricLoader: String by rootProject

val loom = extensions.getByType<LoomGradleExtensionAPI>()
val sourceSets = extensions.getByType<SourceSetContainer>()

val jar = tasks.named<Jar>("jar")
val remapJar = tasks.named<RemapJarTask>("remapJar")

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
    // TODO Update gradle module metadata in FFLoader to avoid this
    "compileOnly"("org.sinytra:fabric-loader:$versionForgifiedFabricLoader")
    "runtimeOnly"("org.sinytra:fabric-loader:$versionForgifiedFabricLoader:full") {
        isTransitive = false
    }

    "testmodImplementation"(mainSourceSet.output)
    "testmodCompileOnly"("org.sinytra:fabric-loader:$versionForgifiedFabricLoader")
    "testmodRuntimeOnly"("org.sinytra:fabric-loader:$versionForgifiedFabricLoader:full") {
        isTransitive = false
    }

    "testImplementation"(testmod.output)
    "testImplementation"("org.mockito:mockito-core:5.4.0")
    "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.8.1")
    "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    enabled = false
}

// Setup AW -> AT conversion
afterEvaluate {
    if (loom.accessWidenerPath.isPresent) {
        // Find the relative AW file name
        var awName: String? = null
        val awPath = loom.accessWidenerPath.get().asFile.toPath()
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

        for (srcDir in main.resources.srcDirs) {
            val srcDirPath = srcDir.toPath().toAbsolutePath()

            if (awPath.startsWith(srcDirPath)) {
                awName = srcDirPath.relativize(awPath).toString().replace(File.separator, "/")
                break
            }
        }

        if (awName == null) {
            awName = awPath.fileName.toString()
        }

        remapJar.get().atAccessWideners.add(awName)
    }
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
            // FIXME Set this from fabric-api-base as ResourcePackProfileMixin fails otherwise
            property("mixin.initialiserInjectionMode", "safe")
        }

        create("gametest") {
            server()
            isIdeConfigGenerated = project.rootProject == project
            name = "Testmod Game Test Server"
            source(testmod)

            // Enable the gametest runner
            property("neoforge.gameTestServer", "true")
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

