dependencies {
    implementation(project(":core"))

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.github.dmulloy2:protocollib:${libs.versions.protocollib.old.get()}")
}
