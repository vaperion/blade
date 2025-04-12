dependencyResolutionManagement {
	@Suppress("UnstableApiUsage")
	repositories {
		mavenCentral()
		maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
		maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
		maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
	}

	versionCatalogs {
		create("libs") {
			version("shadow", "9.0.0-beta9")
			version("lombok", "8.13.1")

			plugin("shadow", "com.gradleup.shadow").versionRef("shadow")
			plugin("lombok", "io.freefair.lombok").versionRef("lombok")

			version("annotations", "26.0.2")
			version("paper", "1.20.4-R0.1-SNAPSHOT")
			version("velocity", "3.0.1")
			version("protocollib", "5.3.0")
		}
	}
}

include(
        "core",
		"bukkit",
		"velocity",
		"paper"
)

rootProject.name = "blade"