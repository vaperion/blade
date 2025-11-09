java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":core"))
    api(project(":brigadier"))
    api(project(":bukkit"))

    compileOnly("io.papermc.paper:paper-api:${libs.versions.paper.new.get()}")
}
