java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":core"))
    api(project(":brigadier"))
    api(project(":bukkit"))

    api(project(":paper-legacy")) {
        exclude(group = "io.papermc.paper", module = "paper-api")
        exclude(group = "io.papermc.paper", module = "paper-mojangapi")
    }

    compileOnly("io.papermc.paper:paper-api:${libs.versions.paper.new.get()}")
}
