import com.google.gson.JsonParser
import net.fabricmc.accesswidener.AccessWidenerReader
import net.fabricmc.accesswidener.AccessWidenerVisitor
import net.fabricmc.accesswidener.AccessWidenerWriter
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.extension.LoomGradleExtensionApiImpl
import net.fabricmc.loom.task.service.MappingsService
import net.fabricmc.loom.task.service.SourceRemapperService
import net.fabricmc.loom.util.DeletingFileVisitor
import net.fabricmc.loom.util.SourceRemapper
import net.fabricmc.loom.util.service.BuildSharedServiceManager
import net.fabricmc.mappingio.tree.MappingTreeView
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.mercury.Mercury
import org.cadixdev.mercury.mixin.MixinRemapper
import org.cadixdev.mercury.remapper.MercuryRemapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

val sourceSets = extensions.getByType<SourceSetContainer>()

val versionFabricLoader: String by project

evaluationDependsOn(":fabric-api-remap")

val remapTask = tasks.register("remapUpstreamSources", RemapSourceDirectory::class) {
    group = "sinytra"

    val projectRoot = rootProject.projectDir.resolve("api-meta/fabric-api-upstream/${project.name}")
    projectRoot.resolve("src").listFiles()?.forEach {
        sourceRoots.from(it.resolve("java"))
        resourceRoots.from(it.resolve("resources"))
    }

    classpath.from(project(":intermediary-deobf").configurations[JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME])

    sourceNamespace.set(MappingsNamespace.NAMED.toString())
    targetNamespace.set(MappingsNamespace.MOJANG.toString())
    outputDir.set(rootProject.file("api-meta/fabric-api-mojmap").resolve("${project.name}/src").apply {
        toPath().createDirectories()
    })
}

abstract class RemapSourceDirectory : DefaultTask() {
    @get:SkipWhenEmpty
    @get:InputFiles
    val sourceRoots: ConfigurableFileCollection = project.objects.fileCollection()

    @get:SkipWhenEmpty
    @get:InputFiles
    val resourceRoots: ConfigurableFileCollection = project.objects.fileCollection()

    @get:InputFiles
    val classpath: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Input
    val sourceNamespace: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val targetNamespace: Property<String> = project.objects.property(String::class.java)

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Inject
    abstract val buildEventsListenerRegistry: BuildEventsListenerRegistry
    
    private val serviceManagerProvider: Provider<BuildSharedServiceManager> =
        BuildSharedServiceManager.createForTask(this, buildEventsListenerRegistry)

    private val sourceMappingService by lazy(::createSourceMapper)

    @Synchronized
    fun createSourceMapper(): SourceRemapperService {
        val serviceManager = serviceManagerProvider.get().get()
        val to = targetNamespace.get()
        val from = sourceNamespace.get()
        val javaCompileRelease = SourceRemapper.getJavaCompileRelease(project)
        return SourceRemapperService::class.java.getDeclaredConstructor(
            MappingsService::class.java,
            ConfigurableFileCollection::class.java,
            Int::class.java
        ).apply { isAccessible = true }.newInstance(
            MappingsService.createDefault(project.project(":fabric-api-remap"), serviceManager, from, to),
            classpath,
            javaCompileRelease
        ) as SourceRemapperService
    }

    @TaskAction
    fun run() {
        val mappingService by lazy {
            MappingsService.createDefault(
                project.project(":fabric-api-remap"),
                serviceManagerProvider.get().get(),
                sourceNamespace.get(),
                targetNamespace.get()
            )
        }

        val mercurySupplier =
            SourceRemapperService::class.java.getDeclaredField("mercury").apply { isAccessible = true }
                .get(sourceMappingService) as Supplier<Mercury>
        val mercury = mercurySupplier.get()
        val mappingSet =
            SourceRemapperService::class.java.getDeclaredMethod("getMappings").apply { isAccessible = true }
                .invoke(sourceMappingService) as MappingSet

        mercury.isGracefulClasspathChecks = false
        mercury.processors.clear()
        mercury.processors.add(MixinRemapper.create(mappingSet))
        mercury.processors.add(MercuryRemapper.create(mappingSet))
        mercury.isFlexibleAnonymousClassMemberLookups = true

        sourceRoots.filter(File::exists).forEach { sourceRoot ->
            val destDir = outputDir.file("${sourceRoot.parentFile.name}/${sourceRoot.name}").get().asFile
            destDir.mkdirs()

            prepareMixinMappings(sourceRoot.toPath(), mappingSet, mercury.classPath)

            sourceMappingService.remapSourcesJar(sourceRoot.toPath(), destDir.toPath())
        }
        resourceRoots.filter(File::exists).forEach { resourceRoot ->
            val fmj = resourceRoot.resolve("fabric.mod.json")
            if (fmj.exists()) {
                val json = fmj.bufferedReader().use(JsonParser::parseReader).asJsonObject
                val accessWidener = json.get("accessWidener")?.asString
                if (accessWidener != null) {
                    val awPath = resourceRoot.resolve(accessWidener).toPath()
                    val destDir = outputDir.file("${resourceRoot.parentFile.name}/${resourceRoot.name}").get().asFile
                    destDir.mkdirs()

                    val mappedPath = destDir.resolve(accessWidener).toPath()

                    val version: Int = awPath.bufferedReader().use { input -> AccessWidenerReader.readVersion(input) }
                    val writer = AccessWidenerWriter(version)
                    val awRemapper = MappingAccessWidenerRemapper(
                        writer,
                        mappingService.memoryMappingTree,
                        MappingsNamespace.NAMED.toString(),
                        MappingsNamespace.MOJANG.toString(),
                        MappingsNamespace.NAMED.toString()
                    )
                    val reader = AccessWidenerReader(awRemapper)

                    awPath.bufferedReader().use(reader::read)
                    mappedPath.writeBytes(writer.write())
                }
            }
        }
    }

    private fun prepareMixinMappings(
        inputFile: Path,
        mappingSet: MappingSet,
        classpath: Collection<Path>
    ) {
        val mercury = Mercury()
        mercury.setSourceCompatibilityFromRelease(SourceRemapper.getJavaCompileRelease(project))
        mercury.isFlexibleAnonymousClassMemberLookups = true
        mercury.processors.add(MixinRemapper.create(mappingSet))
        mercury.classPath += classpath

        val dstDir = Files.createTempDirectory("fabric-loom-dst")

        try {
            mercury.rewrite(inputFile, dstDir)
        } finally {
            Files.walkFileTree(dstDir, DeletingFileVisitor())
        }
    }

    class MappingAccessWidenerRemapper(
        private val delegate: AccessWidenerVisitor,
        private val remapper: MappingTreeView,
        private val fromNamespace: String,
        toNamespace: String,
        private val toHeaderNamespace: String
    ) : AccessWidenerVisitor {
        private val fromNamespaceOrdinal = remapper.getNamespaceId(fromNamespace)
        private val toNamespaceOrdinal = remapper.getNamespaceId(toNamespace)

        override fun visitHeader(namespace: String) {
            require(fromNamespace == namespace) {
                ("Cannot remap access widener from namespace '" + namespace + "'."
                        + " Expected: '" + this.fromNamespace + "'")
            }

            delegate.visitHeader(toHeaderNamespace)
        }

        override fun visitClass(name: String, access: AccessWidenerReader.AccessType, transitive: Boolean) {
            delegate.visitClass(
                remapper.mapClassName(name, fromNamespaceOrdinal, toNamespaceOrdinal),
                access,
                transitive
            )
        }

        override fun visitMethod(
            owner: String,
            name: String,
            descriptor: String,
            access: AccessWidenerReader.AccessType,
            transitive: Boolean
        ) {
            delegate.visitMethod(
                remapper.mapClassName(owner, fromNamespaceOrdinal, toNamespaceOrdinal),
                remapper.getMethod(owner, name, descriptor, fromNamespaceOrdinal)?.getDstName(toNamespaceOrdinal)
                    ?: name,
                remapper.mapDesc(descriptor, fromNamespaceOrdinal, toNamespaceOrdinal),
                access,
                transitive
            )
        }

        override fun visitField(
            owner: String,
            name: String,
            descriptor: String,
            access: AccessWidenerReader.AccessType,
            transitive: Boolean
        ) {
            delegate.visitField(
                remapper.mapClassName(owner, fromNamespaceOrdinal, toNamespaceOrdinal),
                remapper.getField(owner, name, descriptor, fromNamespaceOrdinal)?.getDstName(toNamespaceOrdinal)
                    ?: name,
                remapper.mapDesc(descriptor, fromNamespaceOrdinal, toNamespaceOrdinal),
                access,
                transitive
            )
        }
    }
}
