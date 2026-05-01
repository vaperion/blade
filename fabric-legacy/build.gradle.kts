java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

plugins {
    alias(libs.plugins.loom.remap)
}

dependencies {
    api(project(":core"))
    api(project(":brigadier"))

    minecraft("com.mojang:minecraft:${libs.versions.fabric.legacy.minecraft.get()}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${libs.versions.fabric.legacy.loader.get()}")

    val apiModules = listOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1"
    )

    apiModules.forEach {
        modImplementation(fabricApi.module(it, libs.versions.fabric.legacy.api.get()))
    }

    modCompileOnly("me.lucko:fabric-permissions-api:${libs.versions.lucko.legacy.permissions.get()}")
}

tasks {
    processResources {
        filteringCharset = "UTF-8"

        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version
            )
        }
    }

    remapJar {
        inputFile.set(jar.get().archiveFile)
    }
}
