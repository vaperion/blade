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
    compileOnly("net.dmulloy2:ProtocolLib:${libs.versions.protocollib.new.get()}")
}
