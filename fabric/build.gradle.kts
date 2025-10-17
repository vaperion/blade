java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

plugins {
    alias(libs.plugins.loom)
}

dependencies {
    api(project(":core"))
    api(project(":brigadier"))

    minecraft("com.mojang:minecraft:${libs.versions.fabric.minecraft.get()}")
    mappings("net.fabricmc:yarn:${libs.versions.fabric.mappings.get()}:v2")
    modImplementation("net.fabricmc:fabric-loader:${libs.versions.fabric.loader.get()}")

    val apiModules = listOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1"
    )

    apiModules.forEach {
        modImplementation(fabricApi.module(it, libs.versions.fabric.api.get()))
    }

    modImplementation(
        include("me.lucko:fabric-permissions-api:${libs.versions.lucko.permissions.get()}")!!
    )
}

loom {
    accessWidenerPath = file("src/main/resources/blade.accesswidener")
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
