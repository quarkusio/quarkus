// Source: {{source}}
import io.quarkus.gradle.application.tasks.QuarkusApplicationDevTask
import org.gradle.api.plugins.jvm.JvmTestSuite

// tag::legacy-first-coexistence[]
plugins {
    id("io.quarkus")
    id("io.quarkus.application")
}

group = "org.acme"
version = "1.0.0"

val quarkusPlatformVersion = providers.gradleProperty("quarkusPlatformVersion").get()
val quarkusPlatformGroupId = providers.gradleProperty("quarkusPlatformGroupId")
    .orElse("io.quarkus.platform").get()

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:quarkus-bom:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-rest")
    quarkusDev("io.quarkus:quarkus-jdbc-h2")

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.rest-assured:rest-assured")
}

quarkusApplication {
    builds {
        fastJar("migration") {
            outputName = "acme-migration"
            participatesInAssemble = false
        }
        nativeExecutable("native")
    }
    dev {
        continuousTesting = false
    }
}
// end::legacy-first-coexistence[]

if (providers.gradleProperty("standaloneParticipatesInAssemble").isPresent) {
    quarkusApplication.builds.all {
        if (name == "migration") {
            participatesInAssemble = true
        }
    }
}

if (providers.gradleProperty("verifyNativeIntegrationRestriction").isPresent) {
    testing.suites.register<JvmTestSuite>("nativeIntegrationTest") {
        forQuarkusIntegrationTests("native")
    }
}

val quarkusDevConfiguration = configurations.named("quarkusDev")
val standaloneDev = tasks.named<QuarkusApplicationDevTask>("quarkusApplicationDev")
val standaloneContinuousTest = tasks.named<QuarkusApplicationDevTask>("quarkusApplicationContinuousTest")

tasks.register("verifyMigrationOwnership") {
    doLast {
        check(project.extensions.findByName("quarkus") != null)
        check(project.extensions.findByName("quarkusApplication") != null)
        check(quarkusDevConfiguration.get().isCanBeResolved)
        check(standaloneDev.get().legacyTestsOwned.get())
        check(!standaloneDev.get().continuousTesting.get())
        check(standaloneContinuousTest.get().legacyTestsOwned.get())
        check(standaloneContinuousTest.get().continuousTesting.get())
        check(tasks.findByName("quarkusBuild") != null)
        check(tasks.findByName("quarkusTest") != null)
        check(tasks.findByName("quarkusMigrationBuild") != null)
        check(tasks.findByName("quarkusNativeNativeTest") == null)
        check(tasks.findByName("quarkusGenerateCode") != null)
        check(tasks.findByName("quarkusApplicationGenerateCode") != null)
        val compileJava = tasks.named("compileJava").get()
        val compileDependencies = compileJava.taskDependencies.getDependencies(compileJava).map { it.name }.toSet()
        check("quarkusGenerateCode" in compileDependencies)
        check("quarkusApplicationGenerateCode" in compileDependencies)
        val assemble = tasks.named("assemble").get()
        val assembleDependencies = assemble.taskDependencies.getDependencies(assemble).map { it.name }.toSet()
        check("quarkusBuild" in assembleDependencies)
        check("quarkusMigrationBuild" in assembleDependencies)
    }
}
