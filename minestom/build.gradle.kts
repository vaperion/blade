java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":core"))

    implementation("net.minestom:minestom:${libs.versions.minestom.get()}")
}
