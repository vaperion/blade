plugins {
    alias(libs.plugins.blossom)
    `java-test-fixtures`
}

dependencies {
    compileOnlyApi("org.jetbrains:annotations:${libs.versions.annotations.get()}")

    testImplementation(testFixtures(project(":core")))
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