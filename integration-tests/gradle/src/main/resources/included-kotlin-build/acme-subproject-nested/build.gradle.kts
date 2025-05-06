plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal {
        content {
            includeGroupByRegex("io.quarkus.*")
            includeGroup("org.hibernate.orm")
        }
    }
    mavenCentral()
}

group = "org.acme"
version = "1.0-SNAPSHOT"

kotlin {
    compilerOptions {
        javaParameters = true
    }
}
