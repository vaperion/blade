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

    compileOnly("com.hypixel.hytale:Server:${libs.versions.hytale.get()}")
}
