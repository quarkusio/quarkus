package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;

class QuarkusApplicationContinuousTestingFailureRecoveryTest
        extends QuarkusApplicationContinuousBuildTestSupport {

    private static final String FAILURE_REPORTED = "Reported failure of Gradle task '%s' to the Quarkus continuous session.";
    private static final String WAITING_FOR_CHANGES = "Waiting for changes to input files...";

    @Test
    void continuousTestTaskRerunsTestsAndRecoversFromTestCompilationFailure() throws Exception {
        writeContinuousTestApplication();

        Path receipt = testProjectDir.resolve(Path.of("build", "quarkus-continuous-test", "iteration.properties"));
        Path closeReceipt = testProjectDir.resolve(
                Path.of("build", "quarkus-continuous-test", "session-closed.txt"));
        Path outputSnapshot = testProjectDir.resolve(
                Path.of("build", "quarkus-continuous-test", "output-snapshot.tsv"));
        Path testSource = testProjectDir.resolve("src/test/java/org/acme/GreetingServiceTest.java");

        Path launchDirectory = testProjectDir.resolve("continuous-work");
        Path launchProbe = launchDirectory.resolve("launch-probe.properties");
        try (var build = startContinuousBuild("quarkusApplicationContinuousTest", "--no-quarkus-debug")) {
            build.await("initial continuous-test run", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true")
                            && fileContains(launchProbe,
                                    "workingDirectory=" + canonicalPath(launchDirectory),
                                    "environment=continuous-dsl"));

            writePassingGreetingTest(testSource, "initial rerun");
            build.await("test-only continuous-test rerun", RELOAD_TIMEOUT,
                    () -> fileContains(receipt, "outcome=PENDING,SENT_APPLIED")
                            && occurrences(build.stdout(), "Tests completed at") >= 1);

            String receiptBeforeTestFailure = Files.readString(receipt);
            String snapshotBeforeTestFailure = Files.readString(outputSnapshot);
            long sequenceBeforeTestFailure = receiptSequence(receipt);
            int waitsBeforeTestFailure = occurrences(build.stdout(), WAITING_FOR_CHANGES);
            Files.writeString(testSource, """
                    package org.acme;

                    this is not valid Java
                    """);
            build.await("test compilation failure delivery", RELOAD_TIMEOUT,
                    () -> build.stdout().contains(FAILURE_REPORTED.formatted(":compileTestJava"))
                            && occurrences(build.stdout(), WAITING_FOR_CHANGES) > waitsBeforeTestFailure);
            assertThat(receipt).hasContent(receiptBeforeTestFailure);
            assertThat(outputSnapshot).hasContent(snapshotBeforeTestFailure);

            writePassingGreetingTest(testSource, "recovered rerun");
            build.await("continuous-test recovery", RELOAD_TIMEOUT,
                    () -> fileContains(receipt, "outcome=PENDING,SENT_APPLIED")
                            && receiptSequence(receipt) > sequenceBeforeTestFailure
                            && occurrences(build.stdout(), "Tests completed at") >= 2);
        }
        assertThat(closeReceipt).hasContent("closed\n");
        assertDirectoryCanBeMoved(testProjectDir.resolve("build"));
    }

    @Test
    void devTaskRerunsTestsAndRecoversFromMainCompilationFailure() throws Exception {
        writeContinuousTestApplication();
        Files.createDirectories(testProjectDir.resolve("build/resources/test"));
        Files.writeString(testProjectDir.resolve("build.gradle"), """

                quarkusApplication {
                    dev {
                        continuousTesting = true
                    }
                }
                """, StandardOpenOption.APPEND);

        Path receipt = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-iteration.properties"));
        Path outputSnapshot = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-output-snapshot.tsv"));
        Path closeReceipt = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-session-closed.txt"));
        Path mainSource = testProjectDir.resolve("src/main/java/org/acme/GreetingService.java");
        Path testSource = testProjectDir.resolve("src/test/java/org/acme/GreetingServiceTest.java");

        try (var build = startContinuousBuild("quarkusApplicationDev", "--no-quarkus-debug")) {
            build.await("initial dev-mode continuous-test run", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true")
                            && occurrences(build.stdout(), "Tests completed at") >= 1
                            && occurrences(build.stdout(), WAITING_FOR_CHANGES) >= 1);

            int waitsBeforeTestChange = occurrences(build.stdout(), WAITING_FOR_CHANGES);
            writePassingGreetingTest(testSource, "dev-mode rerun");
            build.await("dev-mode test-only rerun", RELOAD_TIMEOUT,
                    () -> fileContains(receipt, "outcome=PENDING,SENT_APPLIED")
                            && occurrences(build.stdout(), "Tests completed at") >= 2
                            && occurrences(build.stdout(), WAITING_FOR_CHANGES) > waitsBeforeTestChange);

            int waitsBeforeMainFailure = occurrences(build.stdout(), WAITING_FOR_CHANGES);
            String receiptBeforeMainFailure = Files.readString(receipt);
            String snapshotBeforeMainFailure = Files.readString(outputSnapshot);
            long sequenceBeforeMainFailure = receiptSequence(receipt);
            Files.writeString(mainSource, """
                    package org.acme;

                    this is not valid Java
                    """);
            build.await("main compilation failure delivery", RELOAD_TIMEOUT,
                    () -> build.stdout().contains(FAILURE_REPORTED.formatted(":compileJava"))
                            && occurrences(build.stdout(), WAITING_FOR_CHANGES) > waitsBeforeMainFailure);
            assertThat(receipt).hasContent(receiptBeforeMainFailure);
            assertThat(outputSnapshot).hasContent(snapshotBeforeMainFailure);
            assertThat(closeReceipt).doesNotExist();

            int mainTriggeredTestsBeforeRecovery = occurrences(build.stdout(),
                    "due to changes to GreetingService.class");
            int waitsBeforeMainRecovery = occurrences(build.stdout(), WAITING_FOR_CHANGES);
            Files.writeString(mainSource, """
                    package org.acme;

                    import jakarta.enterprise.context.ApplicationScoped;

                    @ApplicationScoped
                    public class GreetingService {
                        public String hello() {
                            return "hello";
                        }

                        public boolean recovered() {
                            return true;
                        }
                    }
                    """);
            build.await("main compilation recovery", RELOAD_TIMEOUT,
                    () -> fileContentDiffers(receipt, receiptBeforeMainFailure)
                            && receiptSequence(receipt) > sequenceBeforeMainFailure
                            && fileContains(receipt, "outcome=PENDING,SENT_APPLIED")
                            && occurrences(build.stdout(),
                                    "due to changes to GreetingService.class") > mainTriggeredTestsBeforeRecovery
                            && occurrences(build.stdout(), WAITING_FOR_CHANGES) > waitsBeforeMainRecovery);
            assertThat(Files.readString(outputSnapshot)).isNotEqualTo(snapshotBeforeMainFailure);
        }
        assertThat(closeReceipt).hasContent("closed\n");
    }

    private static boolean fileContentDiffers(Path file, String previousContent) {
        try {
            return Files.isRegularFile(file) && !Files.readString(file).equals(previousContent);
        } catch (IOException e) {
            return false;
        }
    }

}
