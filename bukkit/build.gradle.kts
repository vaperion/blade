dependencies {
    api(project(":core"))

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")

    api("net.kyori:adventure-api:${libs.versions.adventure.get()}")
    api("net.kyori:adventure-text-serializer-legacy:${libs.versions.adventure.get()}")
}
