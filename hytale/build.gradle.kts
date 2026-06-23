java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://maven.hytale.com/release/")
    }

    maven {
        url = uri("https://repo.codemc.io/repository/ArikSquad/")
    }
}

dependencies {
    api(project(":core"))

    compileOnly("com.hypixel.hytale:Server:${libs.versions.hytale.get()}")

    implementation("eu.mikart.adventure:adventure-platform-hytale:1.0.4")
}
