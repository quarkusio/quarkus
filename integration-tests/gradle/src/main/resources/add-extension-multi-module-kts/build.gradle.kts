buildscript {

    repositories {
        if (System.getProperties().containsKey("maven.repo.local")) {
            maven(url = System.getProperties().get("maven.repo.local")!!)
        } else {
            mavenLocal()
        }
        mavenCentral()
        gradlePluginPortal()
    }

}

plugins {
    java
}

group = "com.quarkus.demo"
version = "1.0"


subprojects {

    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks {
      test {
          dependsOn("cleanTest")
          useJUnitPlatform()
          setForkEvery(1)
      }
    }

    repositories {
        if (System.getProperties().containsKey("maven.repo.local")) {
            maven(url = System.getProperties().get("maven.repo.local")!!)
        } else {
            mavenLocal()
        }
        mavenCentral()
    }
}
