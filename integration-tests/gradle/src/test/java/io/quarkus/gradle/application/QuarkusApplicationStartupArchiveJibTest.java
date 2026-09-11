package io.quarkus.gradle.application;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.gradle.BuildResult;

@DisabledOnOs(OS.WINDOWS)
class QuarkusApplicationStartupArchiveJibTest extends QuarkusApplicationGradleTestBase {

    private static final String CONTAINER_TESTS = "QUARKUS_GRADLE_STARTUP_ARCHIVE_JIB";
    private static final String HOTSPOT_BASE_IMAGE = "QUARKUS_GRADLE_HOTSPOT_25_IMAGE";
    private static final String SEMERU_BASE_IMAGE = "QUARKUS_GRADLE_SEMERU_25_IMAGE";
    private static final String DEFAULT_HOTSPOT_BASE_IMAGE = "eclipse-temurin:25-jre";
    private static final String DEFAULT_SEMERU_BASE_IMAGE = "ibm-semeru-runtimes:open-25-jre";
    private static final Duration COMMAND_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration START_TIMEOUT = Duration.ofMinutes(2);
    private static final String RESPONSE = "hello from startup-archive training";

    @Test
    void jibBuildTrainsAndConsumesHotspotAotCache() throws Exception {
        verifyStartupOptimizedImage(ArchiveType.AOT,
                environmentOrDefault(HOTSPOT_BASE_IMAGE, DEFAULT_HOTSPOT_BASE_IMAGE));
    }

    @Test
    void jibBuildTrainsAndConsumesSemeruScc() throws Exception {
        verifyStartupOptimizedImage(ArchiveType.SCC,
                environmentOrDefault(SEMERU_BASE_IMAGE, DEFAULT_SEMERU_BASE_IMAGE));
    }

    private void verifyStartupOptimizedImage(ArchiveType type, String runtimeBaseImage) throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getenv(CONTAINER_TESTS)),
                () -> "Set " + CONTAINER_TESTS + "=true to run real Jib startup-archive tests");
        CommandResult dockerVersion = command(COMMAND_TIMEOUT, "docker", "version", "--format", "{{.Server.Version}}");
        assumeTrue(dockerVersion.success(),
                () -> "A working Docker daemon is required for real Jib startup-archive tests:\n"
                        + dockerVersion.output());

        String unique = type.name().toLowerCase(Locale.ROOT) + "-"
                + ProcessHandle.current().pid() + "-"
                + UUID.randomUUID().toString().replace("-", "");
        String baseImage = "quarkus-test/gradle-startup-archive-" + unique + ":it";
        String optimizedImage = baseImage + type.imageSuffix();
        String containerName = "quarkus-gradle-startup-archive-" + unique;

        try {
            File projectDirectory = getProjectDir("application-plugin/startup-archive-jib");
            BuildResult result = runApplicationGradleWrapper(projectDirectory,
                    "clean",
                    "quarkusOptimizedStartupOptimizedImageBuild",
                    "-PstartupArchiveType=" + type,
                    "-PstartupArchiveBaseImageReference=" + baseImage,
                    "-PstartupArchiveRuntimeBaseImage=" + runtimeBaseImage);

            assertThat(result.unsuccessfulTasks()).isEmpty();
            assertPreflightReference(projectDirectory.toPath(), "image-build", baseImage);
            assertPreflightReference(projectDirectory.toPath(), "startup-optimized-image-build", optimizedImage);
            Path archive = projectDirectory.toPath().resolve("build/quarkus-builds/optimized/"
                    + "startup-archive-training/StartupArchiveTraining/" + type.archiveName());
            type.assertArchive(archive);

            assertCommandSuccess(command(COMMAND_TIMEOUT, "docker", "image", "inspect", optimizedImage),
                    "Jib did not create the startup-optimized image " + optimizedImage);
            CommandResult imageEnvironment = command(COMMAND_TIMEOUT, "docker", "image", "inspect",
                    "--format={{range .Config.Env}}{{println .}}{{end}}", optimizedImage);
            assertCommandSuccess(imageEnvironment, "Unable to inspect " + optimizedImage);
            assertThat(imageEnvironment.output()).contains(type.runtimeOptionFragment());
            if (type == ArchiveType.SCC) {
                assertThat(imageEnvironment.output()).contains("readonly");
            }

            CommandResult started = command(COMMAND_TIMEOUT, "docker", "run", "--detach", "--rm",
                    "--name", containerName, "--publish", "127.0.0.1::8080", optimizedImage);
            assertCommandSuccess(started, "Unable to start " + optimizedImage);
            int port = awaitContainerPort(containerName);
            assertThat(awaitHttpResponse(containerName, port)).isEqualTo(RESPONSE);
        } finally {
            command(COMMAND_TIMEOUT, "docker", "rm", "--force", containerName);
            command(COMMAND_TIMEOUT, "docker", "image", "rm", "--force", optimizedImage, baseImage);
        }
    }

    private static void assertPreflightReference(Path projectDirectory, String operation, String expected)
            throws IOException {
        Path receipt = projectDirectory.resolve("build/quarkus-build-results/optimized/" + operation
                + "/image-reference-preflight.properties");
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(receipt)) {
            properties.load(reader);
        }
        assertThat(properties.getProperty("image.primary")).isEqualTo(expected);
    }

    private static int awaitContainerPort(String containerName) {
        AtomicInteger port = new AtomicInteger();
        await("published HTTP port for " + containerName)
                .pollInterval(Duration.ofMillis(200))
                .atMost(START_TIMEOUT)
                .until(() -> {
                    CommandResult running = command(COMMAND_TIMEOUT, "docker", "inspect",
                            "--format={{.State.Running}}", containerName);
                    if (!running.success() || !"true".equals(running.output().trim())) {
                        CommandResult logs = command(COMMAND_TIMEOUT, "docker", "logs", "--tail=200", containerName);
                        throw new AssertionError("Container " + containerName + " stopped before serving HTTP:\n"
                                + logs.output());
                    }
                    CommandResult mappedPort = command(COMMAND_TIMEOUT, "docker", "port", containerName, "8080/tcp");
                    if (!mappedPort.success() || mappedPort.output().isBlank()) {
                        return false;
                    }
                    String address = mappedPort.output().trim().lines().findFirst().orElseThrow();
                    port.set(Integer.parseInt(address.substring(address.lastIndexOf(':') + 1)));
                    return true;
                });
        return port.get();
    }

    private static String awaitHttpResponse(String containerName, int port) {
        var response = new StringBuilder();
        await("HTTP /hello from " + containerName)
                .pollInterval(Duration.ofMillis(200))
                .atMost(START_TIMEOUT)
                .ignoreExceptions()
                .until(() -> {
                    CommandResult running = command(COMMAND_TIMEOUT, "docker", "inspect",
                            "--format={{.State.Running}}", containerName);
                    if (!running.success() || !"true".equals(running.output().trim())) {
                        CommandResult logs = command(COMMAND_TIMEOUT, "docker", "logs", "--tail=200", containerName);
                        throw new AssertionError("Container " + containerName + " stopped before serving HTTP:\n"
                                + logs.output());
                    }
                    HttpURLConnection connection = (HttpURLConnection) URI
                            .create("http://127.0.0.1:" + port + "/hello")
                            .toURL()
                            .openConnection();
                    connection.setConnectTimeout(1_000);
                    connection.setReadTimeout(1_000);
                    try {
                        if (connection.getResponseCode() != 200) {
                            return false;
                        }
                        response.setLength(0);
                        try (var input = connection.getInputStream()) {
                            response.append(new String(input.readAllBytes(), UTF_8));
                        }
                        return true;
                    } finally {
                        connection.disconnect();
                    }
                });
        return response.toString();
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static void assertCommandSuccess(CommandResult result, String message) {
        assertThat(result.exitCode())
                .withFailMessage(message + " (exit code %s):\n%s", result.exitCode(), result.output())
                .isZero();
    }

    private static CommandResult command(Duration timeout, String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean exited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                return new CommandResult(-1, "Command timed out: " + String.join(" ", command));
            }
            return new CommandResult(process.exitValue(), new String(process.getInputStream().readAllBytes(), UTF_8));
        } catch (IOException e) {
            return new CommandResult(-1, e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CommandResult(-1, "Interrupted while running: " + String.join(" ", command));
        }
    }

    private enum ArchiveType {
        AOT("app.aot", "-aot", "-XX:AOTCache="),
        SCC("app-scc", "-scc", "-Xshareclasses:name=quarkus-app,cacheDir=");

        private final String archiveName;
        private final String imageSuffix;
        private final String runtimeOptionFragment;

        ArchiveType(String archiveName, String imageSuffix, String runtimeOptionFragment) {
            this.archiveName = archiveName;
            this.imageSuffix = imageSuffix;
            this.runtimeOptionFragment = runtimeOptionFragment;
        }

        private String archiveName() {
            return archiveName;
        }

        private String imageSuffix() {
            return imageSuffix;
        }

        private String runtimeOptionFragment() {
            return runtimeOptionFragment;
        }

        private void assertArchive(Path archive) throws IOException {
            if (this == AOT) {
                assertThat(archive).isRegularFile();
                assertThat(Files.size(archive)).isPositive();
                return;
            }
            assertThat(archive).isDirectory();
            try (var entries = Files.list(archive)) {
                assertThat(entries).isNotEmpty();
            }
        }
    }

    private record CommandResult(int exitCode, String output) {

        private boolean success() {
            return exitCode == 0;
        }
    }
}
