plugins {
    id("io.quarkus")
    kotlin("jvm")
}

// This is a fix as kotlin 1.6.0 does not support Java 18 yet
if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_18)) {
    tasks.quarkusDev {
        compilerArgs = listOf("-jvm-target", "17")
    }
}

dependencies {
    implementation(project(":port"))
    implementation(project(":domain"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.quarkus:quarkus-resteasy")
}
