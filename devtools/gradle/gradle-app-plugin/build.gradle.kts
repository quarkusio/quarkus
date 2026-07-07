plugins {
    id("io.quarkus.devtools.gradle-plugin")
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":gradle-model"))
    implementation("io.quarkus:quarkus-analytics-common")
    implementation(libs.smallrye.config.yaml)

    testImplementation(testFixtures(project(":gradle-model")))
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

tasks.test {
    systemProperty("kotlin_version", libs.versions.kotlin.get())
    systemProperty("ksp_version", libs.versions.ksp.get())
}

val additionalPluginUnderTestClasspath = configurations.create("additionalPluginUnderTestClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add(additionalPluginUnderTestClasspath.name, project(":gradle-extension-plugin"))
    add(additionalPluginUnderTestClasspath.name, project(":gradle-extension-deployment-plugin"))
    add(additionalPluginUnderTestClasspath.name, project(":gradle-application-plugin"))
}

group = "io.quarkus.application"

gradlePlugin {
    plugins.create("quarkusApplicationPlugin") {
        id = "io.quarkus.application"
        implementationClass = "io.quarkus.gradle.application.QuarkusApplicationPlugin"
        displayName = "Quarkus Application Plugin"
        description = "Builds explicit named Quarkus application outputs with a Gradle-native task model"
        tags.addAll("quarkus", "quarkusio", "graalvm")
    }
}

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(additionalPluginUnderTestClasspath)
}
