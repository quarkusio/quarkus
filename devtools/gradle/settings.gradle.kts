plugins {
    id("com.gradle.enterprise") version "3.14.1"
}

gradleEnterprise {
    buildScan {
        // plugin configuration
        //See also: https://docs.gradle.com/enterprise/gradle-plugin/
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishOnFailure()
    }
}

rootProject.name = "quarkus-gradle-plugins"
includeBuild("build-logic")
include("gradle-application-plugin", "gradle-extension-plugin", "gradle-model")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
