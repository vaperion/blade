java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://nexus.lucko.me/repository/maven-hytale/")
    }
}

dependencies {
    api(project(":core"))

    compileOnly("com.hypixel.hytale:HytaleServer:2026.01.13-dcad8778f-SNAPSHOT")
}
