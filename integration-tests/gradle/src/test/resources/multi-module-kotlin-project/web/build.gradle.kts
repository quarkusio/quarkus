plugins {
    id("io.quarkus")
    kotlin("jvm")
}

dependencies {
    implementation(project(":port"))
    implementation(project(":domain"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.quarkus:quarkus-resteasy")
}
