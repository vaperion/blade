java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":core"))

    implementation(project(":bukkit")) {
        exclude(group = "com.comphenix.protocol", module = "ProtocolLib")
    }

    compileOnly("io.papermc.paper:paper-api:${libs.versions.paper.get()}")
    compileOnly("io.papermc.paper:paper-mojangapi:${libs.versions.paper.get()}")
    compileOnly("com.comphenix.protocol:ProtocolLib:${libs.versions.protocollib.get()}")
}
