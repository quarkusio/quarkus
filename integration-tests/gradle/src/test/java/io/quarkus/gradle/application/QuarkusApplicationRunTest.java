package io.quarkus.gradle.application;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.tooling.BuildCancelledException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.wrapper.GradleUserHomeLookup;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.util.IoUtils;

class QuarkusApplicationRunTest extends QuarkusApplicationGradleTestBase {

    // Cold Windows workers can spend several minutes resolving and augmenting before application output starts.
    private static final Duration START_TIMEOUT = isWindows() ? Duration.ofMinutes(10) : Duration.ofMinutes(5);
    private static final Duration STOP_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration CLEANUP_TIMEOUT = Duration.ofSeconds(30);
    private static final int DIAGNOSTIC_TAIL_LENGTH = 8 * 1024;
    private static final Pattern HTTP_PORT = Pattern.compile("Listening on: http://127\\.0\\.0\\.1:(\\d+)");

    @Test
    void namedRunUsesRunModeAdditionalOutputsAndStopsWithoutKillingTheDaemon() throws Exception {
        Path projectDirectory = Files.createTempDirectory("quarkus-application-run");
        IoUtils.copy(getProjectDir("application-plugin/run-mode-and-additional-source-set").toPath(), projectDirectory);
        RecordingOutputStream standardOutput = new RecordingOutputStream();
        RecordingOutputStream errorOutput = new RecordingOutputStream();
        CancellationTokenSource cancellation = GradleConnector.newCancellationTokenSource();
        AsyncBuild operation = new AsyncBuild();
        DiagnosticTimeline timeline = new DiagnosticTimeline();
        long applicationPid = -1;
        long daemonPidBefore = -1;
        int port = -1;
        ProjectConnection connection = null;
        boolean operationStarted = false;
        Throwable testFailure = null;

        try {
            connection = connection(projectDirectory);
            timeline.record("Tooling API connection opened");
            BuildLauncher run = connection.newBuild()
                    .forTasks("quarkusAppRun")
                    .withArguments(arguments())
                    .withCancellationToken(cancellation.token())
                    .setStandardOutput(standardOutput)
                    .setStandardError(errorOutput);
            run.run(operation);
            operationStarted = true;
            timeline.record("quarkusAppRun Tooling API operation started");

            Supplier<String> startupDiagnostics = () -> runDiagnostics(projectDirectory, standardOutput, errorOutput,
                    operation, -1, -1, -1, timeline);
            awaitOutput(standardOutput, operation, "RUN-PARTIAL-PROMPT:\u001b[35mready\u001b[0m", START_TIMEOUT,
                    startupDiagnostics);
            awaitOutput(errorOutput, operation, "RUN-STDERR:\u001b[31mready\u001b[0m", START_TIMEOUT,
                    startupDiagnostics);
            assertThat(operation.isDone()).as("run operation remains active after the partial prompt").isFalse();
            timeline.record("expected stdout and stderr launch probes received");

            port = standardOutput.awaitHttpPort(START_TIMEOUT);
            assertThat(httpGet(port, "/hello")).isEqualTo("Hello from the additional source set");
            applicationPid = readPid(projectDirectory.resolve("build/application-child.pid"));
            daemonPidBefore = readPid(projectDirectory.resolve("build/gradle-daemon-before.txt"));
            timeline.record("application ready with pid " + applicationPid + " and Gradle daemon pid " + daemonPidBefore);

            cancellation.cancel();
            timeline.record("quarkusAppRun Tooling API cancellation requested");
            long launchedApplicationPid = applicationPid;
            int launchedPort = port;
            long initialDaemonPid = daemonPidBefore;
            operation.awaitCancellation(STOP_TIMEOUT,
                    () -> runDiagnostics(projectDirectory, standardOutput, errorOutput, operation, launchedApplicationPid,
                            launchedPort, initialDaemonPid, timeline));
            timeline.record("quarkusAppRun Tooling API operation cancelled");

            await().atMost(STOP_TIMEOUT).until(() -> !isAlive(launchedApplicationPid));
            await().atMost(STOP_TIMEOUT).until(() -> !isHttpAvailable(launchedPort));
            timeline.record("launched application and HTTP endpoint stopped");
            if (!isWindows()) {
                assertThat(projectDirectory.resolve("build/application-stopped.txt"))
                        .content(UTF_8)
                        .isEqualTo("stopped");
            }

            runDaemonPidProbe(connection, projectDirectory, applicationPid, port, daemonPidBefore, timeline);
            long daemonPidAfter = readPid(projectDirectory.resolve("build/gradle-daemon-after.txt"));
            assertThat(daemonPidAfter).isEqualTo(daemonPidBefore);
            timeline.record("Gradle daemon reuse confirmed");
        } catch (Exception | AssertionError failure) {
            testFailure = failure;
            throw failure;
        } finally {
            cancellation.cancel();
            AssertionError operationCleanupFailure = null;
            long cleanupApplicationPid = applicationPid > 0
                    ? applicationPid
                    : readPidForCleanup(projectDirectory.resolve("build/application-child.pid"));
            if (operationStarted) {
                int cleanupPort = port;
                long cleanupDaemonPid = daemonPidBefore;
                operationCleanupFailure = operation.awaitCleanup(CLEANUP_TIMEOUT,
                        () -> runDiagnostics(projectDirectory, standardOutput, errorOutput, operation,
                                cleanupApplicationPid, cleanupPort, cleanupDaemonPid, timeline));
            }
            if (cleanupApplicationPid > 0) {
                stopProcess(cleanupApplicationPid);
            }
            if (connection != null) {
                connection.close();
            }
            IoUtils.recursiveDelete(projectDirectory);
            if (operationCleanupFailure != null) {
                if (testFailure != null) {
                    testFailure.addSuppressed(operationCleanupFailure);
                } else {
                    throw operationCleanupFailure;
                }
            }
        }
    }

    private static void runDaemonPidProbe(ProjectConnection connection, Path projectDirectory, long applicationPid,
            int port, long daemonPidBefore, DiagnosticTimeline timeline) {
        RecordingOutputStream standardOutput = new RecordingOutputStream();
        RecordingOutputStream errorOutput = new RecordingOutputStream();
        CancellationTokenSource cancellation = GradleConnector.newCancellationTokenSource();
        AsyncBuild operation = new AsyncBuild();
        AssertionError probeFailure = null;

        connection.newBuild()
                .forTasks("writeGradleDaemonPidAfter")
                .withArguments(arguments())
                .withCancellationToken(cancellation.token())
                .setStandardOutput(standardOutput)
                .setStandardError(errorOutput)
                .run(operation);
        timeline.record("writeGradleDaemonPidAfter Tooling API operation started");
        Supplier<String> diagnostics = () -> runDiagnostics(projectDirectory, standardOutput, errorOutput, operation,
                applicationPid, port, daemonPidBefore, timeline);
        try {
            operation.awaitSuccess(STOP_TIMEOUT, diagnostics);
            timeline.record("writeGradleDaemonPidAfter Tooling API operation completed");
        } catch (AssertionError failure) {
            probeFailure = failure;
            throw failure;
        } finally {
            if (!operation.isDone()) {
                cancellation.cancel();
                timeline.record("writeGradleDaemonPidAfter Tooling API cancellation requested during cleanup");
            }
            AssertionError cleanupFailure = operation.awaitCleanup(CLEANUP_TIMEOUT, diagnostics);
            if (cleanupFailure != null) {
                if (probeFailure != null) {
                    probeFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    private static ProjectConnection connection(Path projectDirectory) {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(projectDirectory.toFile())
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome());
        String requestedVersion = System.getProperty("quarkus-test-gradle-wrapper-version");
        if (requestedVersion != null) {
            connector.useGradleVersion(requestedVersion);
        }
        return connector.connect();
    }

    private static String[] arguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add("--no-configuration-cache");
        arguments.add("--no-watch-fs");
        arguments.add("--stacktrace");
        arguments.add("--info");
        arguments.add("-Dorg.gradle.console=plain");
        arguments.add("-Dquarkus.analytics.disabled=true");
        String localRepository = System.getProperty("maven.repo.local");
        if (localRepository != null) {
            arguments.add("-Dmaven.repo.local=" + localRepository);
        }
        return arguments.toArray(String[]::new);
    }

    private static String runDiagnostics(Path projectDirectory, RecordingOutputStream standardOutput,
            RecordingOutputStream errorOutput, AsyncBuild operation, long applicationPid, int port, long daemonPidBefore,
            DiagnosticTimeline timeline) {
        Path applicationPidFile = projectDirectory.resolve("build/application-child.pid");
        Path daemonPidBeforeFile = projectDirectory.resolve("build/gradle-daemon-before.txt");
        Path daemonPidAfterFile = projectDirectory.resolve("build/gradle-daemon-after.txt");
        return """
                Diagnostic timeline:
                %s
                Tooling API operation: %s
                Application pid: %s
                Application process: %s
                HTTP port: %s
                Gradle daemon pid before: %s
                Gradle daemon process before: %s
                Application pid file (%s):
                %s
                Gradle daemon pid-before file (%s):
                %s
                Gradle daemon pid-after file (%s):
                %s
                Gradle stdout tail:
                %s
                Gradle stderr tail:
                %s
                """.formatted(
                timeline,
                operation.state(),
                applicationPid > 0 ? applicationPid : "not recorded",
                processState(applicationPid),
                port > 0 ? port : "not recorded",
                daemonPidBefore > 0 ? daemonPidBefore : "not recorded",
                processState(daemonPidBefore),
                applicationPidFile,
                readForDiagnostics(applicationPidFile),
                daemonPidBeforeFile,
                readForDiagnostics(daemonPidBeforeFile),
                daemonPidAfterFile,
                readForDiagnostics(daemonPidAfterFile),
                tail(standardOutput.text()),
                tail(errorOutput.text()));
    }

    private static String processState(long pid) {
        if (pid <= 0) {
            return "not recorded";
        }
        return isAlive(pid) ? "alive" : "not alive";
    }

    private static String readForDiagnostics(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.readString(path, UTF_8) : "<absent>";
        } catch (IOException e) {
            return "<unable to read: " + e + ">";
        }
    }

    private static String tail(String value) {
        if (value.length() <= DIAGNOSTIC_TAIL_LENGTH) {
            return value;
        }
        return "<earlier output omitted>\n" + value.substring(value.length() - DIAGNOSTIC_TAIL_LENGTH);
    }

    private static String httpGet(int port, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString(UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private static boolean isHttpAvailable(int port) {
        try {
            httpGet(port, "/hello");
            return true;
        } catch (IOException | InterruptedException | AssertionError e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static long readPid(Path path) throws IOException {
        return Long.parseLong(Files.readString(path, UTF_8).trim());
    }

    private static long readPidForCleanup(Path path) {
        try {
            return Files.isRegularFile(path) ? readPid(path) : -1;
        } catch (IOException | NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean isAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private static void awaitOutput(RecordingOutputStream output, AsyncBuild operation, String expected, Duration timeout,
            Supplier<String> diagnostics) throws InterruptedException {
        if (output.awaitContains(expected, timeout, operation)) {
            return;
        }
        AssertionError failure = new AssertionError("Expected output before the run operation completed or timed out:\n"
                + expected + "\n\n" + diagnostics.get());
        Throwable operationFailure = operation.failure();
        if (operationFailure != null) {
            failure.initCause(operationFailure);
        }
        throw failure;
    }

    private static void stopProcess(long pid) {
        ProcessHandle.of(pid).filter(ProcessHandle::isAlive).ifPresent(process -> {
            process.destroy();
            try {
                process.onExit().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                process.destroyForcibly();
            }
        });
    }

    private static final class AsyncBuild implements ResultHandler<Void> {

        private final CompletableFuture<Void> completion = new CompletableFuture<>();

        @Override
        public void onComplete(Void result) {
            completion.complete(null);
        }

        @Override
        public void onFailure(GradleConnectionException failure) {
            completion.completeExceptionally(failure);
        }

        private boolean isDone() {
            return completion.isDone();
        }

        private Throwable failure() {
            if (!completion.isCompletedExceptionally()) {
                return null;
            }
            try {
                completion.join();
                return null;
            } catch (java.util.concurrent.CompletionException e) {
                return e.getCause();
            }
        }

        private void awaitCancellation(Duration timeout, Supplier<String> diagnostics) {
            try {
                completion.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                throw new AssertionError("Expected the run operation to be cancelled.\n" + diagnostics.get());
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof BuildCancelledException)) {
                    throw new AssertionError("Run Tooling API operation failed instead of being cancelled.\n"
                            + diagnostics.get(), e.getCause());
                }
            } catch (TimeoutException e) {
                throw new AssertionError("Run Tooling API operation remained active after cancellation.\n"
                        + diagnostics.get(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while awaiting run Tooling API cancellation.\n"
                        + diagnostics.get(), e);
            }
        }

        private void awaitSuccess(Duration timeout, Supplier<String> diagnostics) {
            try {
                completion.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new AssertionError("Tooling API operation failed.\n" + diagnostics.get(), e.getCause());
            } catch (TimeoutException e) {
                throw new AssertionError("Tooling API operation timed out.\n" + diagnostics.get(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while awaiting Tooling API operation.\n" + diagnostics.get(), e);
            }
        }

        private AssertionError awaitCleanup(Duration timeout, Supplier<String> diagnostics) {
            try {
                completion.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                return null;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof BuildCancelledException) {
                    return null;
                }
                return new AssertionError("Tooling API cleanup failed.\n" + diagnostics.get(), e.getCause());
            } catch (TimeoutException e) {
                return new AssertionError("Tooling API operation remained active after cleanup cancellation.\n"
                        + diagnostics.get(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new AssertionError("Interrupted while awaiting Tooling API cleanup.\n" + diagnostics.get(), e);
            }
        }

        private String state() {
            if (!completion.isDone()) {
                return "running";
            }
            try {
                completion.join();
                return "completed successfully";
            } catch (CompletionException e) {
                Throwable failure = e.getCause();
                return "failed with " + failure.getClass().getName() + ": " + failure.getMessage();
            }
        }
    }

    private static final class DiagnosticTimeline {

        private final long started = System.nanoTime();
        private final List<String> events = new ArrayList<>();

        private void record(String event) {
            events.add("+%d ms: %s".formatted(
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), event));
        }

        @Override
        public String toString() {
            return String.join(System.lineSeparator(), events);
        }
    }

    private static final class RecordingOutputStream extends ByteArrayOutputStream {

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            super.write(bytes, offset, length);
            notifyAll();
        }

        private synchronized boolean awaitContains(String expected, Duration timeout, AsyncBuild operation)
                throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (!text().contains(expected)) {
                if (operation.isDone()) {
                    return false;
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                TimeUnit.NANOSECONDS.timedWait(this, Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(100)));
            }
            return true;
        }

        private synchronized int awaitHttpPort(Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            Matcher matcher = HTTP_PORT.matcher(text());
            while (!matcher.find()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new AssertionError("Quarkus HTTP port was not reported in output:\n" + text());
                }
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
                matcher = HTTP_PORT.matcher(text());
            }
            return Integer.parseInt(matcher.group(1));
        }

        private synchronized String text() {
            return toString(UTF_8);
        }
    }
}
