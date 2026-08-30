package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class QuarkusApplicationDevContinuousBuildTest extends QuarkusApplicationContinuousBuildTestSupport {

    @Test
    void devTaskReceivesIncrementalSourceChangesFromGradleContinuousBuild() throws Exception {
        writeContinuousDevApplication();
        Path launchDirectory = Files.createDirectories(testProjectDir.resolve("cli-dev-work"));
        Path launchProbe = launchDirectory.resolve("launch-probe.properties");

        Path closeReceipt = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-session-closed.txt"));

        try (var build = startContinuousBuild(
                "quarkusApplicationDev",
                "--working-directory", "cli-dev-work",
                "--environment", "DEV_LAUNCH_PROBE=first",
                "--environment", "DEV_LAUNCH_PROBE=command-line",
                "--environment", "DEV_LAUNCH_WITH_EQUALS=left=right",
                "--disable-extension-jvm-options-for", "org.acme:first",
                "--disable-extension-jvm-options-for", "org.acme:second",
                "--no-quarkus-debug")) {
            Path receipt = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-iteration.properties"));
            build.await("initial quarkusApplicationDev baseline", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sequence=1", "sessionReady=true", "outcome=BASELINE_DROPPED")
                            && fileContains(launchProbe,
                                    "workingDirectory=" + canonicalPath(launchDirectory),
                                    "environment=command-line",
                                    "withEquals=left=right"));

            Files.writeString(testProjectDir.resolve("src/main/java/org/acme/NewGreetingService.java"), """
                    package org.acme;

                    import jakarta.enterprise.context.ApplicationScoped;

                    @ApplicationScoped
                    public class NewGreetingService {
                        public String hello() {
                            return "new";
                        }
                    }
                    """);

            build.await("second continuous build receipt", RELOAD_TIMEOUT,
                    () -> fileContains(receipt, "sequence=2"));
            assertThat(Files.readString(receipt))
                    .contains("incremental=true")
                    .contains("outcome=PENDING,SENT_APPLIED");
        }
        assertThat(closeReceipt).hasContent("closed\n");
        assertDirectoryCanBeMoved(testProjectDir.resolve("build"));
    }

    @Test
    void devTaskRecoversAfterTheReadyChildExits() throws Exception {
        writeContinuousDevApplication();
        Path workingDirectory = Files.createDirectories(testProjectDir.resolve("recovery-work"));
        Path launchProbe = workingDirectory.resolve("launch-probe.properties");
        Path receipt = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-iteration.properties"));
        Path closeReceipt = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-session-closed.txt"));
        long recoveredPid;

        try (var build = startContinuousBuild(
                "quarkusApplicationDev",
                "--working-directory", "recovery-work",
                "--disable-extension-jvm-options-for", "org.acme:first",
                "--disable-extension-jvm-options-for", "org.acme:second",
                "--no-quarkus-debug")) {
            build.await("initial child generation", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true", "outcome=BASELINE_DROPPED")
                            && processId(launchProbe) > 0);
            long initialPid = processId(launchProbe);
            ProcessHandle initialProcess = ProcessHandle.of(initialPid)
                    .orElseThrow(() -> new AssertionError("Dev child process " + initialPid + " does not exist"));

            assertThat(initialProcess.destroyForcibly()).isTrue();
            initialProcess.onExit().get(10, TimeUnit.SECONDS);

            build.await("automatic child recovery and output rebaseline", RELOAD_TIMEOUT,
                    () -> processId(launchProbe) > 0
                            && processId(launchProbe) != initialPid
                            && fileContains(receipt, "outcome=RECOVERY_REBASELINE,PENDING,SENT_APPLIED")
                            && build.stdout().contains("Recovered Quarkus dev mode with generation 2."));
            recoveredPid = processId(launchProbe);
            assertThat(ProcessHandle.of(recoveredPid)).hasValueSatisfying(process -> assertThat(process.isAlive()).isTrue());

            Files.writeString(testProjectDir.resolve("src/main/java/org/acme/RecoveredService.java"), """
                    package org.acme;

                    import jakarta.enterprise.context.ApplicationScoped;

                    @ApplicationScoped
                    public class RecoveredService {
                        public String value() {
                            return "recovered";
                        }
                    }
                    """);
            build.await("incremental delivery after child recovery", RELOAD_TIMEOUT,
                    () -> fileContains(receipt, "sequence=2", "outcome=PENDING,SENT_APPLIED"));
        }

        assertThat(closeReceipt).hasContent("closed\n");
        assertThat(ProcessHandle.of(recoveredPid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        assertDirectoryCanBeMoved(testProjectDir.resolve("build"));
    }

    @Test
    void oversizedDeltaRebaselinesAndContinuesWithBoundedMessages() throws Exception {
        writeContinuousDevApplication();
        Path workDirectory = Files.createDirectories(testProjectDir.resolve("rebaseline-work"));
        Path startupReceipt = workDirectory.resolve("rebaseline-starts.txt");
        Path sources = testProjectDir.resolve("src/main/java/org/acme");
        Files.writeString(sources.resolve("RebaselineProbe.java"), """
                package org.acme;

                import java.io.IOException;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.StandardOpenOption;

                import jakarta.enterprise.context.ApplicationScoped;
                import jakarta.enterprise.event.Observes;

                import io.quarkus.runtime.StartupEvent;

                @ApplicationScoped
                public class RebaselineProbe {
                    void started(@Observes StartupEvent event) throws IOException {
                        Files.writeString(Path.of("rebaseline-starts.txt"), "started\\n",
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                }
                """);

        Path receipt = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-iteration.properties"));
        Path greeting = sources.resolve("GreetingService.java");
        try (var build = startContinuousBuild(
                "quarkusApplicationDev",
                "-D" + DELTA_MAX_BYTES_PROPERTY + "=1",
                "--working-directory", "rebaseline-work",
                "--disable-extension-jvm-options-for", "org.acme:first",
                "--disable-extension-jvm-options-for", "org.acme:second",
                "--no-quarkus-debug")) {
            build.await("initial rebaseline test baseline", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true")
                            && fileLineCount(startupReceipt) >= 1);

            long initialSequence = receiptSequence(receipt);
            int initialRebaselines = occurrences(build.stdout(), REBASELINE_LOG);
            writeGreetingService(greeting, "one");
            build.await("first bounded rebaseline", RELOAD_TIMEOUT,
                    () -> receiptSequence(receipt) > initialSequence
                            && fileContains(receipt, "outcome=PENDING,SENT_APPLIED")
                            && occurrences(build.stdout(), REBASELINE_LOG) > initialRebaselines
                            && fileLineCount(startupReceipt) >= 2);

            long firstRebaselineSequence = receiptSequence(receipt);
            int firstRebaselines = occurrences(build.stdout(), REBASELINE_LOG);
            writeGreetingService(greeting, "two");
            build.await("continued build after bounded rebaseline", RELOAD_TIMEOUT,
                    () -> receiptSequence(receipt) > firstRebaselineSequence
                            && fileContains(receipt, "outcome=PENDING,SENT_APPLIED")
                            && occurrences(build.stdout(), REBASELINE_LOG) > firstRebaselines
                            && fileLineCount(startupReceipt) >= 3);
        }
    }

    private static long fileLineCount(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.readAllLines(file).size() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private static void writeGreetingService(Path source, String marker) throws IOException {
        Files.writeString(source, """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class GreetingService {
                    public String hello() {
                        return "%s";
                    }
                }
                """.formatted(marker));
    }

    private void writeContinuousDevApplication() throws IOException {
        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'continuous-dev-smoke'\n");
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
                }

                quarkusApplication {
                    dev {
                        workingDirectory.set(layout.projectDirectory.dir("missing-dsl-dev-work"))
                        environmentVariables.put("DEV_LAUNCH_PROBE", "dsl")
                        debug = true
                        debugMode = io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode.CONNECT
                        extensionJvmOptions.disableFor.add("org.acme:artifact:classifier:type")
                    }
                }
                """.formatted(pluginClasspathFiles()));
        Path sources = testProjectDir.resolve("src/main/java/org/acme");
        Files.createDirectories(sources);
        Files.writeString(sources.resolve("GreetingService.java"), """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class GreetingService {
                    public String hello() {
                        return "hello";
                    }
                }
                """);
        Files.writeString(sources.resolve("InitialService.java"), """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class InitialService {
                    public String hello() {
                        return "initial";
                    }
                }
                """);
        writeLaunchProbe(sources);
    }

    private static long processId(Path launchProbe) {
        try {
            if (!Files.isRegularFile(launchProbe)) {
                return -1;
            }
            return Files.readAllLines(launchProbe).stream()
                    .filter(line -> line.startsWith("pid="))
                    .map(line -> line.substring("pid=".length()))
                    .mapToLong(Long::parseLong)
                    .findFirst()
                    .orElse(-1);
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

}
