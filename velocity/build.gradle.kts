dependencies {
    implementation(project(":core"))

    compileOnly("com.velocitypowered:velocity-api:${libs.versions.velocity.get()}")
}