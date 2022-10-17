buildscript {

    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("io.quarkus.*")
            }
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
          systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
      }
    }

    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("io.quarkus.*")
            }
        }
        mavenCentral()
    }
}
