package io.quarkus.gradle.application;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.QuarkusGradleModelFactory;
import io.quarkus.bootstrap.resolver.QuarkusToolingModelResult;
import io.quarkus.bootstrap.resolver.QuarkusToolingModelResult.ProviderKind;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.dependency.ArtifactKey;
import io.smallrye.common.process.ProcessUtil;

class QuarkusApplicationToolingModelIT extends QuarkusApplicationGradleTestBase {

    private static final List<String> ENABLE_JAR_PACKAGING = List.of(
            "-Dorg.gradle.java.compile-classpath-packaging=true");
    private static final long PROCESS_TIMEOUT_MINUTES = 5;
    private static final long PROCESS_FORCE_TIMEOUT_SECONDS = 10;

    @TempDir
    Path testDirectory;

    @Test
    void productionClientLoadsPublishedStandaloneProvider() throws Exception {
        Path projectDirectory = fixture("provider-smoke");

        QuarkusToolingModelResult result = QuarkusGradleModelFactory.createPaired(
                projectDirectory.toFile(), "DEVELOPMENT", List.of());

        assertThat(result.getProviderKind()).isEqualTo(ProviderKind.STANDALONE_APPLICATION);
        assertThat(result.getSidecar()).isNotNull();
        assertDevelopmentModel(result.getApplicationModel());
    }

    @Test
    void testModelBootstrapsAndExecutesFiniteIdeStyleQuarkusTest() throws Exception {
        Path projectDirectory = fixture("finite-junit");

        QuarkusToolingModelResult result = QuarkusGradleModelFactory.createPaired(
                projectDirectory.toFile(), "TEST", ENABLE_JAR_PACKAGING, "classes", "testClasses");

        assertThat(result.getProviderKind()).isEqualTo(ProviderKind.STANDALONE_APPLICATION);
        assertThat(result.getApplicationModel().getApplicationModule().getTestSources()).isNotNull();
        assertThat(projectDirectory.resolve(
                "build/classes/java/test/org/acme/application/ToolingModelQuarkusTest.class")).isRegularFile();

        Path processOutput = projectDirectory.resolve("finite-junit-output.log");
        Process process = finiteTestProcess(projectDirectory)
                .redirectErrorStream(true)
                .redirectOutput(processOutput.toFile())
                .start();
        ProcessWaitResult waitResult = waitForAndCleanup(process, processOutput,
                PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES,
                PROCESS_FORCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        boolean finished = waitResult.finished();
        boolean terminated = waitResult.terminated();

        String output = Files.readString(processOutput, UTF_8);
        assertThat(terminated)
                .withFailMessage("Finite Quarkus JUnit process remained alive after forced termination. PID: %s.%n"
                        + "Process output:%n%s", process.pid(), output)
                .isTrue();
        assertThat(finished)
                .withFailMessage("Finite Quarkus JUnit process did not finish. Process output:%n%s", output)
                .isTrue();
        assertThat(process.exitValue())
                .withFailMessage("Finite Quarkus JUnit process failed. Process output:%n%s", output)
                .isZero();
        assertThat(output).contains(FiniteQuarkusTestLauncher.SUCCESS_MARKER);
    }

    @Test
    void forcedTestProcessCleanupRemainsBoundedWhenTheProcessDoesNotExit() throws Exception {
        ForceResistantProcess process = new ForceResistantProcess();

        assertThat(forceAndAwait(process, 1, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(process.destroyForciblyCalled).isTrue();
    }

    @Test
    void interruptedTestProcessWaitForcesTheChildAndPreservesInterruption() {
        InterruptingProcess process = new InterruptingProcess();

        try {
            assertThatThrownBy(() -> waitForAndCleanup(process,
                    testDirectory.resolve("unused-process-output.log"),
                    1, TimeUnit.MILLISECONDS,
                    1, TimeUnit.MILLISECONDS))
                    .isInstanceOf(InterruptedException.class);
            assertThat(process.destroyForciblyCalled).isTrue();
            assertThat(process.isAlive()).isFalse();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptedTestProcessReportsOutputWhenTerminationCannotBeConfirmed() throws Exception {
        Path processOutput = testDirectory.resolve("interrupted-process-output.log");
        Files.writeString(processOutput, "captured process output", UTF_8);
        InterruptingForceResistantProcess process = new InterruptingForceResistantProcess();

        try {
            assertThatThrownBy(() -> waitForAndCleanup(process, processOutput,
                    1, TimeUnit.MILLISECONDS,
                    1, TimeUnit.MILLISECONDS))
                    .isInstanceOfSatisfying(InterruptedException.class, failure -> assertThat(failure.getSuppressed())
                            .anySatisfy(suppressed -> assertThat(suppressed)
                                    .hasMessageContaining("PID: 42")
                                    .hasMessageContaining("captured process output")
                                    .hasMessageContaining("still alive")));
            assertThat(process.destroyForciblyCalled).isTrue();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    static ProcessWaitResult waitForAndCleanup(Process process, Path processOutput,
            long initialTimeout, TimeUnit initialUnit,
            long forceTimeout, TimeUnit forceUnit) throws InterruptedException {
        try {
            boolean finished = process.waitFor(initialTimeout, initialUnit);
            return new ProcessWaitResult(finished, finished || forceAndAwait(process, forceTimeout, forceUnit));
        } catch (InterruptedException failure) {
            // Timed Process.waitFor clears the interrupt status when it throws.
            // forceAndAwait restores it, so clear it again if the interruption
            // happened during forced cleanup and perform one bounded final attempt.
            Thread.interrupted();
            boolean terminated = !process.isAlive();
            if (!terminated) {
                try {
                    terminated = forceAndAwait(process, forceTimeout, forceUnit);
                } catch (InterruptedException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                    Thread.interrupted();
                    terminated = !process.isAlive();
                }
            }
            if (!terminated) {
                failure.addSuppressed(processStillAliveFailure(process, processOutput));
            }
            Thread.currentThread().interrupt();
            throw failure;
        }
    }

    private static IllegalStateException processStillAliveFailure(Process process, Path processOutput) {
        String output;
        try {
            output = Files.readString(processOutput, UTF_8);
        } catch (IOException e) {
            output = "<unable to read captured output: " + e.getMessage() + ">";
        }
        return new IllegalStateException(
                "Finite Quarkus JUnit process is still alive after interrupted forced termination. PID: "
                        + process.pid() + ".\nProcess output:\n" + output);
    }

    static boolean forceAndAwait(Process process, long timeout, TimeUnit unit) throws InterruptedException {
        process.destroyForcibly();
        try {
            return process.waitFor(timeout, unit) || !process.isAlive();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    record ProcessWaitResult(boolean finished, boolean terminated) {
    }

    private Path fixture(String name) throws IOException {
        Path projectDirectory = testDirectory.resolve(name);
        IoUtils.copy(getProjectDir("application-plugin/tooling-model").toPath(), projectDirectory);
        return projectDirectory;
    }

    private static ProcessBuilder finiteTestProcess(Path projectDirectory) {
        String testClasspath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        String fixtureClasspath = String.join(File.pathSeparator,
                testClasspath,
                projectDirectory.resolve("build/classes/java/test").toString(),
                projectDirectory.resolve("build/resources/test").toString(),
                projectDirectory.resolve("build/classes/java/main").toString(),
                projectDirectory.resolve("build/resources/main").toString(),
                projectDirectory.resolve("library/build/classes/java/main").toString(),
                projectDirectory.resolve("library/build/resources/main").toString());
        List<String> command = new ArrayList<>();
        command.add(ProcessUtil.pathOfJava().toString());
        command.add("-Dorg.gradle.console=plain");
        command.add("-Dquarkus.analytics.disabled=true");
        command.add("-Dquarkus.http.test-port=0");
        command.add("-Dio.quarkus.force-color-support=false");
        String localRepository = System.getProperty("maven.repo.local");
        if (localRepository != null) {
            command.add("-Dmaven.repo.local=" + localRepository);
        }
        command.add("-cp");
        command.add(fixtureClasspath);
        command.add(FiniteQuarkusTestLauncher.class.getName());
        command.add("org.acme.application.ToolingModelQuarkusTest");
        return new ProcessBuilder(command)
                .directory(projectDirectory.toFile());
    }

    private static void assertDevelopmentModel(ApplicationModel model) {
        assertThat(model.getAppArtifact().getGroupId()).isEqualTo("org.acme");
        assertThat(model.getAppArtifact().getArtifactId()).isEqualTo("tooling-model-application");
        assertThat(model.getDependencies())
                .extracting(dependency -> ArtifactKey.of(
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getClassifier(),
                        dependency.getType()))
                .contains(ArtifactKey.of("org.acme", "library"));
    }

    private static final class ForceResistantProcess extends Process {

        private boolean destroyForciblyCalled;

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            throw new InterruptedException("Unbounded wait is not allowed");
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public int exitValue() {
            throw new IllegalThreadStateException("Process is still running");
        }

        @Override
        public void destroy() {
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalled = true;
            return this;
        }

        @Override
        public boolean isAlive() {
            return true;
        }
    }

    private static final class InterruptingProcess extends Process {

        private boolean alive = true;
        private boolean destroyForciblyCalled;

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            throw new InterruptedException("wait interrupted");
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            if (alive) {
                throw new InterruptedException("wait interrupted");
            }
            return true;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("Process is still running");
            }
            return 0;
        }

        @Override
        public void destroy() {
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalled = true;
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }
    }

    private static final class InterruptingForceResistantProcess extends Process {

        private boolean firstWait = true;
        private boolean destroyForciblyCalled;

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            throw new InterruptedException("Unbounded wait is not allowed");
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            if (firstWait) {
                firstWait = false;
                throw new InterruptedException("wait interrupted");
            }
            return false;
        }

        @Override
        public int exitValue() {
            throw new IllegalThreadStateException("Process is still running");
        }

        @Override
        public void destroy() {
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalled = true;
            return this;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public long pid() {
            return 42;
        }
    }
}
