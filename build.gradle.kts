import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.lombok)
    `java-library`
    `maven-publish`
}

group = "me.vaperion.blade"
version = "3.0.20"

subprojects {
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.isFork = true
        options.encoding = "UTF-8"
    }

    tasks.named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        archiveFileName.set("blade-${project.name}.jar")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifact(tasks.named<ShadowJar>("shadowJar"))
            }
        }
    }

    tasks.named("build") {
        dependsOn(tasks.named("shadowJar"))
    }
}