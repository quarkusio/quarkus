plugins {
    id("com.gradle.develocity") version "4.0"
}

develocity {
    val isAuthenticated = !System.getenv("DEVELOCITY_ACCESS_KEY").isNullOrEmpty()
    if(isAuthenticated) {
        server = "https://ge.quarkus.io"
    }

    buildScan {
        if (!isAuthenticated) {
            termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
            termsOfUseAgree.set("yes")
        }
        publishing.onlyIf { it.buildResult.failures.isNotEmpty() }
        uploadInBackground = System.getenv("CI").isNullOrEmpty()
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
