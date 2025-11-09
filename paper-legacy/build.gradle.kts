java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    api(project(":core"))
    api(project(":brigadier"))
    api(project(":bukkit"))

    compileOnly("io.papermc.paper:paper-api:${libs.versions.paper.legacy.get()}")
    compileOnly("io.papermc.paper:paper-mojangapi:${libs.versions.paper.legacy.get()}")
}
