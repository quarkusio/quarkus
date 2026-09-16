import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType
import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    id("io.quarkus.application")
}

group = "org.acme.gradledemo"
version = "1.0.0"

val quarkusPlatformVersion = providers.gradleProperty("quarkusPlatformVersion").get()

repositories {
    mavenLocal() // only need Maven local for this demo - not for production use cases
    mavenCentral()
}

dependencies {
    // Production should prefer io.quarkus.platform:quarkus-bom instead
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-container-image-jib")

    implementation(project(":api"))
    implementation(project(":dogs-service"))

    // Our demo extension, from the included Gradle build under demo-extension/
    implementation("org.acme.gradledemo:demo-greeting-extension:1.0.0")
}

////////////////////////////////////////////////////////////////////////////////////////////
// For the demo: when build/task inputs change
//
// A message configured here, emitted via "Java source template" file
// app/src/demo-template/java/org/acme/gradledemo/DemoBuildMessage.java
val demoBuildMessage = "Build script says hello"
val generatedDemoSourceDirectory = layout.buildDirectory.dir("generated/sources/demo-message/main/java")

val generateDemoBuildMessage = tasks.register<Copy>("generateDemoBuildMessage") {
    from("src/demo-template/java")
    into(generatedDemoSourceDirectory)
    inputs.property("demoBuildMessage", demoBuildMessage)
    expand("demoBuildMessage" to demoBuildMessage)
}

sourceSets.named("main") {
    java.srcDir(generatedDemoSourceDirectory)
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateDemoBuildMessage)
}

////////////////////////////////////////////////////////////////////////////////////////////

quarkusApplication {
    builds {

        // Some fast-jar build named 'fast'.
        // Gradle task `quarkusFastBuild` builds the fast-jar.
        // 'quarkus-run.jar' et al land in build/quarkus-builds/fast/package
        //                                                      ----
        //                                       build-name ------^
        fastJar("fast")

        // Native image build named 'native'.
        // Gradle task `quarkusNativeImageBuild`
        //     builds the native binary and the container image
        // Gradle task `quarkusNativeBuild`
        //     builds just the native binary
        nativeExecutable("native")

        // Some fast-jar build named 'container'
        fastJar("container") {
            image {
                repository = "quarkus-gradle-demo"
                builder = QuarkusApplicationImageBuilder.JIB
            }
        }

        // Produces the startup-optimized image via the
        // `quarkusAotStartupOptimizedImageBuild` Gradle task.
        // AOT training is either performed via "testing" or by supplying training data.
        aotJar("aot", QuarkusApplicationJvmStartupArchiveType.AOT) {
            image {
                repository = "quarkus-gradle-demo"
                builder = QuarkusApplicationImageBuilder.JIB
                // need the "right" JDK for AOT
                quarkusBuildProperties.put(
                    "quarkus.jib.base-jvm-image",
                    "registry.access.redhat.com/ubi10/openjdk-25-runtime:1.24"
                )
            }
            startupOptimizedImage {
                imageSuffix = "-aot"
            }
        }
    }
}

// Gradle's test-suites
testing {
    suites {
        register<JvmTestSuite>("aotTraining") {
            useJUnitJupiter("5.13.4")
            dependencies {
                implementation(enforcedPlatform("io.quarkus:quarkus-bom:$quarkusPlatformVersion"))
                implementation("io.quarkus:quarkus-junit")
            }
            targets.all {
                testTask.configure {
                    javaLauncher = javaToolchains.launcherFor {
                        languageVersion = JavaLanguageVersion.of(25)
                    }
                }
            }

            // This "wires" the Gradle JVM test suite to the
            // named 'aot' Quarkus build above.
            forQuarkusIntegrationTests("aot")

            startupArchiveTraining {
                executionTarget = QuarkusApplicationStartupArchiveTrainingExecutionTarget.BASE_IMAGE
            }
        }
    }
}
