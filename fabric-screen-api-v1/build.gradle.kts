val withClientSourceSet: () -> Unit by extra
val withTestMod: () -> Unit by extra

withClientSourceSet()
withTestMod()

mixin {
    add(sourceSets.main.get(), "fabric-screen-api-v1-refmap.json")
    config("fabric-screen-api-v1.mixins.json")
}

dependencies {
    referenceApi(group = "net.fabricmc.fabric-api", name = project.name, version = "1.0.47+3bd4ab0ff4")
    
    api(project(":fabric-api-base"))
}