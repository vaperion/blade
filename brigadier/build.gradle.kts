java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    api(project(":core"))

    compileOnly("com.mojang:brigadier:${libs.versions.brigadier.get()}")
}
