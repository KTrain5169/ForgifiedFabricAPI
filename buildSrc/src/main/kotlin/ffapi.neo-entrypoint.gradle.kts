import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import net.fabricmc.loader.impl.metadata.DependencyOverrides
import net.fabricmc.loader.impl.metadata.EntrypointMetadata
import net.fabricmc.loader.impl.metadata.LoaderModMetadata
import net.fabricmc.loader.impl.metadata.ModMetadataParser
import net.fabricmc.loader.impl.metadata.VersionOverrides
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

val versionMc: String by rootProject
val versionForge: String by rootProject

// TODO use org/sinytra everywhere + migrate loader
val generatedPackageName = "org.sinytra.fabric.generated"

val loom = extensions.getByType<LoomGradleExtensionAPI>()
val sourceSets = extensions.getByType<SourceSetContainer>()

// Source sets that can contain the mod entrypoint file
val masterSourceSets = listOf("main", "testmod").mapNotNull(sourceSets::findByName)

masterSourceSets.forEach { sourceSet ->
    val taskName = sourceSet.getTaskName("generate", "ForgeModEntrypoint")
    val targetDir = project.file("src/generated/${sourceSet.name}/java")
    val task = tasks.register(taskName, GenerateForgeModEntrypoint::class.java) {
        group = "fabric"
        description = "Generates entrypoint files for ${sourceSet.name} fabric mod."

        // Only apply to default source directory since we also add the generated
        // sources to the source set.
        sourceRoots.from(sourceSet.java.srcDirs)
        outputDir.set(targetDir)
        fabricModJson.set(file("src/${sourceSet.name}/resources/fabric.mod.json"))
        packageName.set(generatedPackageName)
    }
    sourceSet.java.srcDir(task)
}

abstract class GenerateForgeModEntrypoint : DefaultTask() {
    @get:SkipWhenEmpty
    @get:InputFiles
    val sourceRoots: ConfigurableFileCollection = project.objects.fileCollection()

    @get:InputFile
    val fabricModJson: RegularFileProperty = project.objects.fileProperty()

    @get:Input
    val packageName: Property<String> = project.objects.property(String::class)

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun run() {
        val className = "GeneratedEntryPoint"
        val packagePath = packageName.get().replace('/', '.')
        val packageDir = outputDir.file(packagePath).get().asFile.toPath()
        packageDir.createDirectories()
        val destFile = packageDir.resolve("$className.java")

        val modMetadata = parseModMetadata(fabricModJson.asFile.get())
        val modid = normalizeModid(modMetadata.id)
        val commonEntrypoints = modMetadata.getEntrypoints("main").map(EntrypointMetadata::getValue).filter(::entryPointExists).map { "new $it().onInitialize();" }
        val clientEntrypoints = modMetadata.getEntrypoints("client").map(EntrypointMetadata::getValue).filter(::entryPointExists).map { "new $it().onInitializeClient();" }
        val serverEntrypoints = modMetadata.getEntrypoints("server").map(EntrypointMetadata::getValue).filter(::entryPointExists).map { "new $it().onInitializeServer();" }

        val commonEntrypointInit = if (commonEntrypoints.isNotEmpty()) {
            """// Initialize main entrypoints
                    ${commonEntrypoints.joinToString(separator = "\n")}"""
        } else ""
        val clientEntrypointInit = if (clientEntrypoints.isNotEmpty()) {
            """
                    // Initialize client entrypoints
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        ${clientEntrypoints.joinToString(separator = "\n")}
                    }"""
        } else ""
        val serverEntrypointInit = if (serverEntrypoints.isNotEmpty()) {
            """
                    // Initialize server entrypoints
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isDedicatedServer()) {
                        ${serverEntrypoints.joinToString(separator = "\n")}
                    }"""
        } else ""
        val entrypointInitializers = listOf(commonEntrypointInit, clientEntrypointInit, serverEntrypointInit)
            .filter(String::isNotEmpty)
            .joinToString(separator = "\n                    ")

        val template = """
            package ${packageName.get()};
            
            @net.neoforged.fml.common.Mod($className.MOD_ID)
            public class $className {
                public static final String MOD_ID = "$modid";  
                public static final String RAW_MOD_ID = "${modMetadata.id}";  
            
                public $className() {
                    $entrypointInitializers
                }
            }
        """.trimIndent()

        destFile.writeText(template)
    }

    private fun entryPointExists(path: String): Boolean {
        return sourceRoots.any { root -> root.resolve(path.replace('.', '/') + ".java").exists() }
    }

    private fun normalizeModid(modid: String): String {
        return modid.replace('-', '_')
    }

    private fun parseModMetadata(file: File): LoaderModMetadata {
        return file.inputStream().use {
            ModMetadataParser.parseMetadata(
                it,
                "",
                listOf(),
                VersionOverrides(),
                DependencyOverrides(project.file("nonexistent").toPath()),
                false
            )
        }
    }
}