plugins {
    alias(libs.plugins.blossom)
}

dependencies {
    compileOnlyApi("org.jetbrains:annotations:${libs.versions.annotations.get()}")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
            }
        }
    }
}

tasks.processResources {
    dependsOn(tasks.generateTemplates)
}