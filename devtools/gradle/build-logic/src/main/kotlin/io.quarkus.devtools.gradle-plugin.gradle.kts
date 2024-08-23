plugins {
    id("io.quarkus.devtools.java-library")
    id("com.gradle.plugin-publish")
    id("maven-publish")
}

dependencies {
    val libs = project.the<VersionCatalogsExtension>().named("libs")
    implementation(project(":gradle-model"))
    implementation(libs.getLibrary("quarkus-devtools-common"))
}

gradlePlugin {
    website.set("https://quarkus.io/")
    vcsUrl.set("https://github.com/quarkusio/quarkus")
}
