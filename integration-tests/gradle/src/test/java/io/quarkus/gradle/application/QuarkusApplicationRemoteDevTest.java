package io.quarkus.gradle.application;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.awaitility.core.ConditionTimeoutException;
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
import io.quarkus.deployment.dev.remotedev.HttpRemoteDevPackageClient;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientConfig;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientOutcome;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageDiff;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageSnapshot;
import io.smallrye.common.process.ProcessUtil;

class QuarkusApplicationRemoteDevTest extends QuarkusApplicationGradleTestBase {

    private static final Duration START_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration RELOAD_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration STOP_TIMEOUT = Duration.ofMinutes(2);
    private static final int DIAGNOSTIC_TAIL_LENGTH = 8 * 1024;
    private static final String WAITING_FOR_CHANGES = "Waiting for changes to input files...";
    private static final Pattern HTTP_PORT = Pattern.compile("Listening on: http://127\\.0\\.0\\.1:(\\d+)");
    private static final String PASSWORD = "remote-dev-password-must-not-leak";
    private static final String RECONNECT_MARKER_PATH = "dev/app/META-INF/resources/reconnect-marker.txt";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Test
    void remoteDevSynchronizesARealMutableJarAcrossAnApplicationModelRestart() throws Exception {
        Path testDirectory = Files.createTempDirectory("quarkus-application-remote-dev");
        Path remoteDirectory = testDirectory.resolve("remote");
        Path localDirectory = testDirectory.resolve("local");
        Path remoteOutput = testDirectory.resolve("remote.log");
        IoUtils.copy(getProjectDir("application-plugin/remote-dev").toPath(), remoteDirectory);
        IoUtils.copy(remoteDirectory, localDirectory);
        String reconnectMarker = Files.readString(
                localDirectory.resolve("src/main/resources/META-INF/resources/reconnect-marker.txt"), UTF_8);

        Path localSource = localDirectory.resolve("src/main/java/org/acme/GreetingResource.java");
        replace(localSource, "remote-initial", "local-initial");

        Process remote = null;
        ProjectConnection connection = null;
        CancellationTokenSource cancellation = GradleConnector.newCancellationTokenSource();
        AsyncBuild operation = new AsyncBuild();
        RecordingOutputStream standardOutput = new RecordingOutputStream();
        RecordingOutputStream errorOutput = new RecordingOutputStream();
        DiagnosticTimeline timeline = new DiagnosticTimeline();
        boolean operationStarted = false;
        boolean cancelled = false;
        Throwable testFailure = null;

        try {
            runApplicationGradleWrapper(remoteDirectory.toFile(), "quarkusApplicationRemoteDevBuild");
            timeline.record("remote package built");
            Path remotePackage = remoteDirectory.resolve("build/quarkus-remote-dev/build");
            Path remoteJar = remotePackage.resolve("quarkus-run.jar");
            assertThat(remoteJar).isRegularFile();
            List<Path> removableHealthFiles = packageFilesContaining(remotePackage, "smallrye-health");
            assertThat(removableHealthFiles).isNotEmpty();

            int port = availablePort();
            remote = startRemote(remoteJar, remoteOutput, port);
            timeline.record("remote JVM started with pid " + remote.pid());
            assertThat(awaitHttpPort(remote, remoteOutput)).isEqualTo(port);
            awaitHttpBody(remote, remoteOutput, port, "remote-initial");
            timeline.record("remote JVM accepted the initial HTTP request");

            connection = connection(localDirectory);
            timeline.record("Tooling API connection opened");
            BuildLauncher remoteDev = connection.newBuild()
                    .forTasks("quarkusApplicationRemoteDev")
                    .withArguments(arguments(port))
                    .withCancellationToken(cancellation.token())
                    .setStandardOutput(standardOutput)
                    .setStandardError(errorOutput);
            remoteDev.run(operation);
            operationStarted = true;
            timeline.record("remote-dev Tooling API operation started");

            Path receipt = localDirectory.resolve("build/quarkus-remote-dev/build-result/remote-dev-result.properties");
            awaitReceiptSequence(receipt, 1, operation, standardOutput, errorOutput, remote, remoteOutput, timeline);
            timeline.record("received remote-dev receipt sequence 1");
            awaitHttpBody(remote, remoteOutput, port, "local-initial");

            Path localPackage = localDirectory.resolve("build/quarkus-remote-dev/build");
            Path localReconnectMarker = localPackage.resolve(RECONNECT_MARKER_PATH);
            Path remoteReconnectMarker = remotePackage.resolve(RECONNECT_MARKER_PATH);
            assertThat(localReconnectMarker).content(UTF_8).isEqualTo(reconnectMarker);
            awaitHttpBody(remote, remoteOutput, port, "/reconnect-marker.txt", reconnectMarker);

            int sequenceBeforeExternalReconnect = receiptSequence(receipt);
            RemoteDevPackageSnapshot localSnapshot = RemoteDevPackageSnapshot.capture(localPackage);
            try (var competingClient = new HttpRemoteDevPackageClient(new RemoteDevPackageClientConfig(
                    URI.create("http://127.0.0.1:" + port), Optional.of(PASSWORD)))) {
                assertThat(competingClient.connect(localSnapshot.hashes()).outcome())
                        .isEqualTo(RemoteDevPackageClientOutcome.CONNECTED);
                assertThat(competingClient.send(new RemoteDevPackageDiff(List.of(), List.of(RECONNECT_MARKER_PATH))).outcome())
                        .isEqualTo(RemoteDevPackageClientOutcome.SENT);
            }
            timeline.record("competing client invalidated the Gradle session and deleted the reconnect marker");
            awaitHttpStatus(remote, remoteOutput, port, "/reconnect-marker.txt", 404);
            await().alias("remote reconnect marker deletion")
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(RELOAD_TIMEOUT)
                    .untilAsserted(() -> assertThat(remoteReconnectMarker).doesNotExist());

            awaitReceiptSequence(receipt, sequenceBeforeExternalReconnect + 1, operation, standardOutput, errorOutput,
                    remote, remoteOutput, timeline);
            timeline.record("received automatically triggered no-edit reconnect receipt");
            await().alias("unchanged requested reconnect marker restoration")
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(RELOAD_TIMEOUT)
                    .untilAsserted(() -> assertThat(remoteReconnectMarker).content(UTF_8).isEqualTo(reconnectMarker));
            awaitHttpBody(remote, remoteOutput, port, "/reconnect-marker.txt", reconnectMarker);

            int sequenceBeforeSourceUpdate = receiptSequence(receipt);
            int waitsBeforeSourceUpdate = occurrences(standardOutput.text(), WAITING_FOR_CHANGES);
            replace(localSource, "local-initial", "source-update");
            timeline.record("initial source update written");
            awaitReceiptSequence(receipt, sequenceBeforeSourceUpdate + 1, operation, standardOutput, errorOutput, remote,
                    remoteOutput, timeline);
            timeline.record("received remote-dev receipt after the initial source update");
            awaitHttpBody(remote, remoteOutput, port, "source-update");
            awaitOutputOccurrence(standardOutput, errorOutput, operation, receipt, remote, remoteOutput, timeline,
                    WAITING_FOR_CHANGES, waitsBeforeSourceUpdate + 1);

            int startsBeforeModelChange = occurrences(read(remoteOutput), " started in ");
            int sequenceBeforeModelChange = receiptSequence(receipt);
            Path buildFile = localDirectory.resolve("build.gradle");

            removeHealthDependency(buildFile);
            timeline.record("application-model change written");

            awaitReceiptSequenceAfterModelChange(receipt, sequenceBeforeModelChange + 1, operation,
                    standardOutput, errorOutput, remote, remoteOutput, buildFile, removableHealthFiles, timeline);
            timeline.record("received remote-dev receipt after the application-model change");
            await().alias("remote package dependency deletion")
                    .pollInterval(Duration.ofMillis(200))
                    .atMost(RELOAD_TIMEOUT)
                    .untilAsserted(() -> assertThat(removableHealthFiles).noneMatch(Files::exists));
            await().alias("remote application-model restart")
                    .pollInterval(Duration.ofMillis(200))
                    .atMost(RELOAD_TIMEOUT)
                    .untilAsserted(() -> assertThat(occurrences(read(remoteOutput), " started in "))
                            .isGreaterThan(startsBeforeModelChange));

            int sequenceAfterModelChange = receiptSequence(receipt);
            replace(localSource, "source-update", "model-update");
            timeline.record("post-restart source update written");
            awaitReceiptSequence(receipt, sequenceAfterModelChange + 1, operation, standardOutput, errorOutput,
                    remote, remoteOutput, timeline);
            timeline.record("received remote-dev receipt after the post-restart source update");
            awaitHttpBody(remote, remoteOutput, port, "model-update");

            cancellation.cancel();
            timeline.record("remote-dev Tooling API cancellation requested");
            Process remoteProcessAtCancellation = remote;
            operation.awaitStoppedAfterCancellation(STOP_TIMEOUT,
                    () -> remoteDevDiagnostics(
                            localDirectory.resolve("build/quarkus-remote-dev/build-result/remote-dev-result.properties"),
                            operation, standardOutput, errorOutput, remoteProcessAtCancellation, remoteOutput, timeline));
            cancelled = true;
            timeline.record("remote-dev Tooling API operation stopped");
            Path closeReceipt = localDirectory.resolve("build/quarkus-remote-dev/snapshot/session-closed.txt");
            await().alias("remote-dev session close receipt")
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(STOP_TIMEOUT)
                    .untilAsserted(() -> assertThat(closeReceipt).content(UTF_8).isEqualTo("closed\n"));

            assertThat(secretBearingOutputs(localDirectory, remoteOutput, standardOutput, errorOutput))
                    .doesNotContain(PASSWORD);
        } catch (Exception | AssertionError failure) {
            testFailure = failure;
            throw failure;
        } finally {
            AssertionError operationCleanupFailure = null;
            if (operationStarted && !cancelled) {
                cancellation.cancel();
                timeline.record("remote-dev Tooling API cancellation requested during cleanup");
                Process remoteProcess = remote;
                operationCleanupFailure = operation.awaitCleanup(Duration.ofSeconds(30),
                        () -> remoteDevDiagnostics(
                                localDirectory.resolve("build/quarkus-remote-dev/build-result/remote-dev-result.properties"),
                                operation, standardOutput, errorOutput, remoteProcess, remoteOutput, timeline));
            }
            if (connection != null) {
                connection.close();
            }
            try {
                terminate(remote, remoteOutput);
            } finally {
                IoUtils.recursiveDelete(testDirectory);
            }
            if (operationCleanupFailure != null) {
                if (testFailure != null) {
                    testFailure.addSuppressed(operationCleanupFailure);
                } else {
                    throw operationCleanupFailure;
                }
            }
        }
    }

    private static Process startRemote(Path jar, Path output, int port) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                ProcessUtil.pathOfJava().toString(),
                "-Dquarkus.http.host=127.0.0.1",
                "-Dquarkus.http.port=" + port,
                "-Dio.quarkus.force-color-support=false",
                "-Dquarkus.live-reload.password=" + PASSWORD,
                "-jar",
                jar.toString())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile());
        processBuilder.environment().put("QUARKUS_LAUNCH_DEVMODE", "true");
        return processBuilder.start();
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
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

    private static String[] arguments(int port) {
        List<String> arguments = new ArrayList<>();
        arguments.add("--continuous");
        arguments.add("--no-configuration-cache");
        arguments.add("--stacktrace");
        arguments.add("--info");
        arguments.add("-Dorg.gradle.console=plain");
        arguments.add("-Dquarkus.analytics.disabled=true");
        arguments.add("-Dquarkus.live-reload.url=http://127.0.0.1:" + port);
        arguments.add("-Dquarkus.live-reload.password=" + PASSWORD);
        String localRepository = System.getProperty("maven.repo.local");
        if (localRepository != null) {
            arguments.add("-Dmaven.repo.local=" + localRepository);
        }
        return arguments.toArray(String[]::new);
    }

    private static int awaitHttpPort(Process process, Path output) {
        AtomicInteger port = new AtomicInteger();
        await().alias("remote Quarkus listening address")
                .pollInterval(Duration.ofMillis(100))
                .atMost(START_TIMEOUT)
                .untilAsserted(() -> {
                    assertRunning(process, output);
                    Matcher matcher = HTTP_PORT.matcher(read(output));
                    assertThat(matcher.find()).as(() -> read(output)).isTrue();
                    port.set(Integer.parseInt(matcher.group(1)));
                });
        return port.get();
    }

    private static void awaitHttpBody(Process process, Path output, int port, String expected) {
        awaitHttpBody(process, output, port, "/hello", expected);
    }

    private static void awaitHttpBody(Process process, Path output, int port, String path, String expected) {
        await().alias("HTTP " + path + " response " + expected)
                .pollInterval(Duration.ofMillis(200))
                .atMost(RELOAD_TIMEOUT)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertRunning(process, output);
                    HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();
                    HttpResponse<String> response = HTTP_CLIENT.send(request,
                            HttpResponse.BodyHandlers.ofString(UTF_8));
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.body()).isEqualTo(expected);
                });
    }

    private static void awaitHttpStatus(Process process, Path output, int port, String path, int expected) {
        await().alias("HTTP " + path + " status " + expected)
                .pollInterval(Duration.ofMillis(200))
                .atMost(RELOAD_TIMEOUT)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertRunning(process, output);
                    HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();
                    assertThat(HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode())
                            .isEqualTo(expected);
                });
    }

    private static void assertRunning(Process process, Path output) {
        assertThat(process.isAlive())
                .as(() -> "Remote Quarkus process exited. Output:\n" + read(output))
                .isTrue();
    }

    private static void awaitReceiptSequence(Path receipt, int minimumSequence, AsyncBuild operation,
            RecordingOutputStream standardOutput, RecordingOutputStream errorOutput, Process remote, Path remoteOutput,
            DiagnosticTimeline timeline) {
        try {
            await().alias("remote-dev receipt sequence " + minimumSequence)
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(RELOAD_TIMEOUT)
                    .untilAsserted(() -> {
                        operation.assertRunning(standardOutput, errorOutput);
                        assertThat(receiptSequence(receipt)).isGreaterThanOrEqualTo(minimumSequence);
                    });
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(remoteDevDiagnostics(receipt, minimumSequence, operation, standardOutput, errorOutput,
                    remote, remoteOutput, timeline), e);
        }
    }

    private static void awaitReceiptSequenceAfterModelChange(Path receipt, int minimumSequence, AsyncBuild operation,
            RecordingOutputStream standardOutput, RecordingOutputStream errorOutput, Process remote, Path remoteOutput,
            Path buildFile, List<Path> removablePackageFiles, DiagnosticTimeline timeline) {
        try {
            await().alias("remote-dev receipt sequence " + minimumSequence + " after application-model change")
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(RELOAD_TIMEOUT)
                    .untilAsserted(() -> {
                        operation.assertRunning(standardOutput, errorOutput);
                        assertThat(receiptSequence(receipt)).isGreaterThanOrEqualTo(minimumSequence);
                    });
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    modelChangeDiagnostics(receipt, minimumSequence, operation, standardOutput, errorOutput,
                            remote, remoteOutput, buildFile, removablePackageFiles, timeline),
                    e);
        }
    }

    private static String modelChangeDiagnostics(Path receipt, int minimumSequence,
            AsyncBuild operation, RecordingOutputStream standardOutput, RecordingOutputStream errorOutput, Process remote,
            Path remoteOutput, Path buildFile, List<Path> removablePackageFiles, DiagnosticTimeline timeline) {
        List<String> remainingFiles = removablePackageFiles.stream()
                .filter(Files::exists)
                .map(Path::toString)
                .toList();
        return """
                            %s
                Application-model build file (%s, %s):
                %s
                            Still-present removable package files:
                            %s
                            """.formatted(
                remoteDevDiagnostics(receipt, minimumSequence, operation, standardOutput, errorOutput, remote, remoteOutput,
                        timeline),
                buildFile,
                fileState(buildFile),
                diagnosticTail(read(buildFile)),
                remainingFiles);
    }

    private static String remoteDevDiagnostics(Path receipt, int minimumSequence, AsyncBuild operation,
            RecordingOutputStream standardOutput, RecordingOutputStream errorOutput, Process remote, Path remoteOutput,
            DiagnosticTimeline timeline) {
        return "Timed out waiting for remote-dev receipt sequence %d.%n%s".formatted(
                minimumSequence,
                remoteDevDiagnostics(receipt, operation, standardOutput, errorOutput, remote, remoteOutput, timeline));
    }

    private static String remoteDevDiagnostics(Path receipt, AsyncBuild operation,
            RecordingOutputStream standardOutput, RecordingOutputStream errorOutput, Process remote, Path remoteOutput,
            DiagnosticTimeline timeline) {
        String remoteState = remote == null ? "not started"
                : remote.isAlive() ? "alive with pid " + remote.pid() : "exited with code " + remote.exitValue();
        Path packageSnapshot = receipt.getParent().getParent().resolve("snapshot").resolve("package-snapshot.tsv");
        return """
                Diagnostic timeline:
                %s
                Tooling API operation: %s
                Remote process: %s
                Receipt (%s):
                %s
                Package snapshot (%s):
                %s
                Gradle stdout tail:
                %s
                Gradle stderr tail:
                %s
                Remote JVM output tail:
                %s
                """.formatted(
                timeline,
                diagnosticTail(operation.state()),
                remoteState,
                receipt,
                diagnosticTail(read(receipt)),
                packageSnapshot,
                diagnosticTail(read(packageSnapshot)),
                diagnosticTail(standardOutput.text()),
                diagnosticTail(errorOutput.text()),
                diagnosticTail(read(remoteOutput)));
    }

    private static void awaitOutputOccurrence(RecordingOutputStream standardOutput, RecordingOutputStream errorOutput,
            AsyncBuild operation, Path receipt, Process remote, Path remoteOutput, DiagnosticTimeline timeline,
            String expected, int minimumOccurrences) {
        try {
            await().alias("Gradle output occurrence " + expected)
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(RELOAD_TIMEOUT)
                    .untilAsserted(() -> {
                        operation.assertRunning(standardOutput, errorOutput);
                        assertThat(occurrences(standardOutput.text(), expected)).isGreaterThanOrEqualTo(minimumOccurrences);
                    });
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(remoteDevDiagnostics(receipt, operation, standardOutput, errorOutput, remote,
                    remoteOutput, timeline), e);
        }
    }

    private static int receiptSequence(Path receipt) {
        if (!Files.isRegularFile(receipt)) {
            return 0;
        }
        Properties properties = new Properties();
        try (var input = Files.newInputStream(receipt)) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read remote-dev receipt " + receipt, e);
        }
        return Integer.parseInt(properties.getProperty("sequence", "0"));
    }

    private static List<Path> packageFilesContaining(Path packageRoot, String fragment) throws IOException {
        try (Stream<Path> paths = Files.walk(packageRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> packageRoot.relativize(path).toString().contains(fragment))
                    .toList();
        }
    }

    private static void removeHealthDependency(Path buildFile) throws IOException {
        String current = Files.readString(buildFile, UTF_8);
        String dependency = "    implementation 'io.quarkus:quarkus-smallrye-health'";
        assertThat(current).containsOnlyOnce(dependency);
        String updated = current.replace(dependency, "");
        assertThat(updated).isNotEqualTo(current);
        Files.writeString(buildFile, updated, UTF_8);
    }

    private static void replace(Path file, String expected, String replacement) throws IOException {
        String current = Files.readString(file, UTF_8);
        String updated = current.replace(expected, replacement);
        assertThat(updated).isNotEqualTo(current);
        Files.writeString(file, updated, UTF_8);
    }

    private static String secretBearingOutputs(Path localDirectory, Path remoteOutput,
            RecordingOutputStream standardOutput, RecordingOutputStream errorOutput) throws IOException {
        StringBuilder outputs = new StringBuilder()
                .append(read(remoteOutput))
                .append(standardOutput.text())
                .append(errorOutput.text());
        for (Path path : List.of(
                localDirectory.resolve("build/quarkus-remote-dev/build-result/remote-dev-result.properties"),
                localDirectory.resolve("build/quarkus-remote-dev/snapshot/package-snapshot.tsv"),
                localDirectory.resolve("build/quarkus-remote-dev/snapshot/session-closed.txt"))) {
            if (Files.isRegularFile(path)) {
                outputs.append(Files.readString(path, UTF_8));
            }
        }
        return outputs.toString();
    }

    private static int occurrences(String value, String fragment) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(fragment, offset)) >= 0) {
            count++;
            offset += fragment.length();
        }
        return count;
    }

    private static String read(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.readString(file, UTF_8) : "";
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + file, e);
        }
    }

    private static String fileState(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return "not a regular file";
            }
            return "size=%d, modified=%s".formatted(Files.size(file), Files.getLastModifiedTime(file));
        } catch (IOException e) {
            return "unavailable: " + e.getMessage();
        }
    }

    private static String tail(String value) {
        if (value.length() <= DIAGNOSTIC_TAIL_LENGTH) {
            return value;
        }
        return "<earlier output omitted>\n" + value.substring(value.length() - DIAGNOSTIC_TAIL_LENGTH);
    }

    private static String diagnosticTail(String value) {
        return tail(value).replace(PASSWORD, "<redacted>");
    }

    private static void terminate(Process process, Path output) throws InterruptedException {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        if (!process.waitFor(STOP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            assertThat(process.waitFor(STOP_TIMEOUT.toSeconds(), TimeUnit.SECONDS))
                    .as(() -> "Unable to stop remote Quarkus process. Output:\n" + read(output))
                    .isTrue();
        }
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

        private void assertRunning(RecordingOutputStream standardOutput, RecordingOutputStream errorOutput) {
            if (!completion.isDone()) {
                return;
            }
            try {
                completion.get();
                throw new IllegalStateException("Remote-dev continuous build completed unexpectedly.\nstdout:\n"
                        + diagnosticTail(standardOutput.text()) + "\nstderr:\n" + diagnosticTail(errorOutput.text()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while checking remote-dev continuous build", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Remote-dev continuous build failed.\nstdout:\n"
                        + diagnosticTail(standardOutput.text()) + "\nstderr:\n" + diagnosticTail(errorOutput.text()),
                        e.getCause());
            }
        }

        private void awaitStoppedAfterCancellation(Duration timeout, Supplier<String> diagnostics) {
            try {
                completion.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof BuildCancelledException)) {
                    throw new AssertionError("Remote-dev Tooling API operation failed during cancellation.\n"
                            + diagnostics.get(), e.getCause());
                }
            } catch (TimeoutException e) {
                throw new AssertionError("Remote-dev Tooling API operation remained active after cancellation.\n"
                        + diagnostics.get(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while awaiting remote-dev Tooling API cancellation.\n"
                        + diagnostics.get(), e);
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
                return new AssertionError("Remote-dev Tooling API cleanup failed.\n" + diagnostics.get(), e.getCause());
            } catch (TimeoutException e) {
                return new AssertionError("Remote-dev Tooling API operation remained active after cancellation.\n"
                        + diagnostics.get(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new AssertionError("Interrupted while awaiting remote-dev Tooling API cleanup.\n"
                        + diagnostics.get(), e);
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
        public synchronized void write(int value) {
            super.write(value);
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            super.write(bytes, offset, length);
        }

        private synchronized String text() {
            return toString(UTF_8);
        }
    }
}
