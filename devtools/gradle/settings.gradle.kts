plugins {
    id("com.gradle.develocity") version "4.5.0"
}

develocity {
	val isAuthenticated = providers.environmentVariable("DEVELOCITY_ACCESS_KEY")
		.map(String::isNotEmpty)
		.getOrElse(false)
    if(isAuthenticated) {
        server = "https://ge.quarkus.io"
    }

    buildScan {
        if (!isAuthenticated) {
            termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
            termsOfUseAgree.set("yes")
        }
        publishing.onlyIf { it.buildResult.failures.isNotEmpty() }
		uploadInBackground = providers.environmentVariable("CI")
			.map(String::isEmpty)
			.getOrElse(true)
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
