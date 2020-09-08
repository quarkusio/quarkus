buildscript {

    repositories {
        jcenter()
        mavenLocal()
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks {
      test {
          dependsOn("cleanTest")
          useJUnitPlatform()
          setForkEvery(1)
      }
    }

    repositories {
        jcenter()
        mavenLocal()
        mavenCentral()
    }
}
