plugins {
    id("com.gradle.enterprise") version "3.16.2"
}

gradleEnterprise {
    // plugin configuration
    //See also: https://docs.gradle.com/enterprise/gradle-plugin/

    val isAuthenticated = !System.getenv("GRADLE_ENTERPRISE_ACCESS_KEY").isNullOrEmpty()
    if(isAuthenticated) {
        server = "https://ge.quarkus.io"
    }

    buildScan {
        if (!isAuthenticated) {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
        publishOnFailure()
        isUploadInBackground = System.getenv("CI").isNullOrEmpty()
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
