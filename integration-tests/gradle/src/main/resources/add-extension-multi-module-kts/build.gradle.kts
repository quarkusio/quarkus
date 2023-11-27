buildscript {

    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("io.quarkus.*")
                includeGroup("org.hibernate.orm")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
                includeGroup("org.hibernate.orm")
            }
        }
        mavenCentral()
    }
}
