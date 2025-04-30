plugins {
    `java-platform`
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

javaPlatform.allowDependencies()
dependencies{
    api(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    constraints{
        api("org.assertj:assertj-core:3.26.3")
    }
}
