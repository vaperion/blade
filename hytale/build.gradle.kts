java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://maven.hytale.com/release/")
    }
}

dependencies {
    api(project(":core"))

    compileOnly("com.hypixel.hytale:Server:2026.02.17-255364b8e")
}
