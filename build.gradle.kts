plugins {
    alias(libs.plugins.lombok)
    alias(libs.plugins.publishing)
    `java-library`
    `maven-publish`
}

group = "io.github.vaperion.blade"
version = "1.0.0-rc.8"

subprojects {
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.vanniktech.maven.publish")
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

    mavenPublishing {
        publishToMavenCentral()
        signAllPublications()

        coordinates(
            groupId = project.group.toString(),
            artifactId = project.name,
            version = project.version.toString()
        )

        pom {
            name.set(project.name)
            description.set("Blade is an easy-to-use command framework based on annotations.")

            inceptionYear.set("2021")
            url.set("https://github.com/vaperion/blade/")

            licenses {
                license {
                    name.set("The Unlicense")
                    url.set("http://opensource.org/license/unlicense")
                    distribution.set("http://opensource.org/license/unlicense")
                }
            }

            developers {
                developer {
                    id.set("vaperion")
                    name.set("vaperion")
                    url.set("https://github.com/vaperion")
                }
            }

            scm {
                url.set("https://github.com/vaperion/blade")
                connection.set("scm:git:git://github.com/vaperion/blade.git")
                developerConnection.set("scm:git:ssh://git@github.com/vaperion/blade.git")
            }
        }
    }

}