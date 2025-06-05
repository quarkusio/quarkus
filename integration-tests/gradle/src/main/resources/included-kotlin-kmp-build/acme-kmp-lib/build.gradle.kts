plugins {
    kotlin("multiplatform")
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

dependencies {
}

group = "org.acme"
version = "1.0-SNAPSHOT"

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

kotlin {
    jvm()
    js {
        browser()
    }
}
