pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://maven.fabricmc.net/") }
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://libraries.minecraft.net") }
    }

    versionCatalogs {
        create("libs", Action {
            version("lombok", "8.13.1")
            version("loom", "1.11-SNAPSHOT")
            version("publishing", "0.34.0")
            version("blossom", "2.2.0")

            plugin("lombok", "io.freefair.lombok").versionRef("lombok")
            plugin("loom", "fabric-loom").versionRef("loom")
            plugin("publishing", "com.vanniktech.maven.publish").versionRef("publishing")
            plugin("blossom", "net.kyori.blossom").versionRef("blossom")

            version("annotations", "26.0.2")
            version("paper.legacy", "1.20.4-R0.1-SNAPSHOT")
            version("paper.new", "1.20.6-R0.1-SNAPSHOT")
            version("velocity", "3.0.1")

            version("brigadier", "1.0.18")

            version("fabric.minecraft", "1.21.10")
            version("fabric.mappings", "1.21.10+build.2")
            version("fabric.loader", "0.17.3")
            version("fabric.api", "0.135.0+1.21.10")
            version("lucko.permissions", "0.3.3")

            version("minestom", "2025.10.04-1.21.8")
        })
    }
}

include(
    "core",
    "brigadier",
    "bukkit",
    "velocity",
    "paper",
    "paper-legacy",
    "fabric",
    "minestom",
    "hytale"
)

rootProject.name = "blade"
