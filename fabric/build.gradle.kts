java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

plugins {
    alias(libs.plugins.loom)
}

dependencies {
    api(project(":core"))
    api(project(":brigadier"))

    minecraft("com.mojang:minecraft:${libs.versions.fabric.minecraft.get()}")

    implementation("net.fabricmc:fabric-loader:${libs.versions.fabric.loader.get()}")

    val adventure = libs.versions.adventure.get()
    implementation("net.kyori:adventure-api:$adventure")
    implementation("net.kyori:adventure-key:$adventure")
    implementation("net.kyori:adventure-text-serializer-gson:$adventure")
    implementation("net.kyori:examination-api:1.3.0")
    include("net.kyori:adventure-api:$adventure")
    include("net.kyori:adventure-key:$adventure")
    include("net.kyori:adventure-text-serializer-gson:$adventure")
    include("net.kyori:examination-api:1.3.0")

    val apiModules = listOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1"
    )

    apiModules.forEach {
        implementation(fabricApi.module(it, libs.versions.fabric.api.get()))
    }

    compileOnly("me.lucko:fabric-permissions-api:${libs.versions.lucko.permissions.get()}")
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
}
