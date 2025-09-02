plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

group = "org.acme"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()
    js {
        nodejs()
    }
}
