package io.quarkus.gradle.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class QuarkusApplicationContinuousBuildTestSupport extends ContinuousBuildTestSupport {

    static final String REBASELINE_LOG = "Rebaselining Quarkus dev mode from the current external build outputs.";
    static final String DELTA_MAX_BYTES_PROPERTY = "io.quarkus.deployment.dev.build-output-delta-max-bytes";

    static long receiptSequence(Path receipt) {
        try {
            if (!Files.isRegularFile(receipt)) {
                return -1;
            }
            return Files.readAllLines(receipt).stream()
                    .filter(line -> line.startsWith("sequence="))
                    .map(line -> line.substring("sequence=".length()))
                    .mapToLong(Long::parseLong)
                    .findFirst()
                    .orElse(-1);
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    final void writeContinuousTestApplication() throws IOException {
        Files.createDirectories(testProjectDir.resolve("continuous-work"));
        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'continuous-test-smoke'\n");
        Files.writeString(testProjectDir.resolve("gradle.properties"), "version = 999-SNAPSHOT\n");
        Files.writeString(testProjectDir.resolve("build.gradle"), """
                buildscript {
                    dependencies {
                        classpath files(%s)
                    }
                }

                apply plugin: 'io.quarkus.application'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.property('version')}")
                    implementation "io.quarkus:quarkus-arc"
                    testImplementation "io.quarkus:quarkus-junit:${project.property('version')}"
                }

                quarkusApplication {
                    dev {
                        workingDirectory.set(layout.projectDirectory.dir("continuous-work"))
                        environmentVariables.put("DEV_LAUNCH_PROBE", "continuous-dsl")
                    }
                }
                """.formatted(pluginClasspathFiles()));
        Path mainSources = testProjectDir.resolve("src/main/java/org/acme");
        Files.createDirectories(mainSources);
        Files.writeString(mainSources.resolve("GreetingService.java"), """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class GreetingService {
                    public String hello() {
                        return "hello";
                    }
                }
                """);
        writeLaunchProbe(mainSources);
        Path testSource = testProjectDir.resolve("src/test/java/org/acme/GreetingServiceTest.java");
        Files.createDirectories(testSource.getParent());
        writePassingGreetingTest(testSource, "initial run");
    }

    static void writeLaunchProbe(Path sources) throws IOException {
        Files.writeString(sources.resolve("LaunchProbe.java"), """
                package org.acme;

                import java.io.IOException;
                import java.nio.file.Files;
                import java.nio.file.Path;

                import jakarta.enterprise.context.ApplicationScoped;
                import jakarta.enterprise.event.Observes;

                import io.quarkus.runtime.StartupEvent;

                @ApplicationScoped
                public class LaunchProbe {
                    void started(@Observes StartupEvent event) throws IOException {
                        Files.writeString(Path.of("launch-probe.properties"), String.join("\\n",
                                "pid=" + ProcessHandle.current().pid(),
                                "workingDirectory=" + Path.of("").toAbsolutePath().normalize(),
                                "environment=" + System.getenv("DEV_LAUNCH_PROBE"),
                                "withEquals=" + System.getenv("DEV_LAUNCH_WITH_EQUALS"),
                                ""));
                    }
                }
                """);
    }

    static void writePassingGreetingTest(Path testSource, String displayName) throws IOException {
        Files.writeString(testSource, """
                package org.acme;

                import static org.junit.jupiter.api.Assertions.assertEquals;

                import jakarta.inject.Inject;

                import org.junit.jupiter.api.DisplayName;
                import org.junit.jupiter.api.Test;

                import io.quarkus.test.junit.QuarkusTest;

                @QuarkusTest
                class GreetingServiceTest {
                    @Inject
                    GreetingService greetingService;

                    @Test
                    @DisplayName("%s")
                    void greeting() {
                        assertEquals("hello", greetingService.hello());
                    }
                }
                """.formatted(displayName));
    }

}
