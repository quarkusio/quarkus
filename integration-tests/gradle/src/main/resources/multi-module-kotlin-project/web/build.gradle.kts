plugins {
    id("io.quarkus")
    kotlin("jvm")
}

// This is a fix as kotlin 1.5.30 does not support Java 17 yet
if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    tasks.quarkusDev {
        compilerArgs = listOf("-jvm-target", "16")
    }
}
dependencies {
    implementation(project(":port"))
    implementation(project(":domain"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.quarkus:quarkus-resteasy")
}
