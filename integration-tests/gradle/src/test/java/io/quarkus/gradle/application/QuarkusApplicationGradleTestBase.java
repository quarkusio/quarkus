package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.awaitility.core.ConditionTimeoutException;

import io.quarkus.gradle.BuildResult;
import io.quarkus.gradle.QuarkusGradleWrapperTestBase;
import io.smallrye.common.process.ProcessUtil;

public abstract class QuarkusApplicationGradleTestBase extends QuarkusGradleWrapperTestBase {

    private static final String ISOLATED_PROJECTS = "-Dorg.gradle.unsafe.isolated-projects=true";
    private static final Duration START_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern LISTENING_ADDRESS = Pattern.compile("Listening on:\\s+https?://[^:]*:(\\d+)");

    protected BuildResult runApplicationGradleWrapper(File projectDir, String... args)
            throws IOException, InterruptedException {
        List<String> applicationArgs = new ArrayList<>(Arrays.asList(args));
        applicationArgs.add(ISOLATED_PROJECTS);
        return runGradleWrapper(projectDir, applicationArgs.toArray(String[]::new));
    }

    protected void assertJarApplication(Path jar, Path output, Map<String, String> environment,
            String expectedResponse) throws Exception {
        assertJarApplication(ProcessUtil.pathOfJava(), jar, List.of(), output, environment, expectedResponse);
    }

    protected void assertJarApplication(Path javaExecutable, Path jar, List<String> jvmArguments, Path output,
            Map<String, String> environment, String expectedResponse) throws Exception {
        assertThat(javaExecutable).isExecutable();
        assertThat(jar).isRegularFile();
        List<String> command = new ArrayList<>();
        command.add(javaExecutable.toString());
        command.addAll(jvmArguments);
        command.addAll(List.of(
                "-Dquarkus.http.host=127.0.0.1",
                "-Dquarkus.http.port=0",
                "-Dio.quarkus.force-color-support=false",
                "-jar",
                jar.toString()));
        assertApplication(command, output, environment, expectedResponse);
    }

    protected void assertNativeApplication(Path executable, Path output, Map<String, String> environment,
            String expectedResponse) throws Exception {
        assertThat(executable).isRegularFile();
        Map<String, String> nativeEnvironment = new HashMap<>(environment);
        nativeEnvironment.put("QUARKUS_HTTP_HOST", "127.0.0.1");
        nativeEnvironment.put("QUARKUS_HTTP_PORT", "0");
        assertApplication(List.of(executable.toString()), output, nativeEnvironment, expectedResponse);
    }

    private void assertApplication(List<String> command, Path output, Map<String, String> environment,
            String expectedResponse) throws Exception {
        Files.createDirectories(output.getParent());
        Files.deleteIfExists(output);

        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(output.toFile());
        processBuilder.environment().putAll(environment);

        Process process = processBuilder.start();
        try {
            int port = awaitListeningPort(process, output);
            String response = awaitHttpResponse(process, port, output);
            assertThat(response).isEqualTo(expectedResponse);

            String logs = readLogs(output);
            assertThat(logs)
                    .contains("Quarkus ")
                    .contains(" started in ")
                    .contains("Installed features:")
                    .contains("rest");
        } finally {
            terminate(process, output);
        }
    }

    private static int awaitListeningPort(Process process, Path output) throws IOException {
        AtomicInteger port = new AtomicInteger();
        try {
            await("Quarkus listening address")
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(START_TIMEOUT)
                    .until(() -> {
                        if (!process.isAlive()) {
                            throw new AssertionError(
                                    "Quarkus exited before reporting a listening address. Process output:\n"
                                            + readLogs(output));
                        }
                        Matcher matcher = LISTENING_ADDRESS.matcher(readLogs(output));
                        if (!matcher.find()) {
                            return false;
                        }
                        port.set(Integer.parseInt(matcher.group(1)));
                        return true;
                    });
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    "Quarkus did not report a listening address. Process output:\n" + readLogs(output), e);
        }
        return port.get();
    }

    private static String awaitHttpResponse(Process process, int port, Path output) throws IOException {
        AtomicReference<String> response = new AtomicReference<>();
        try {
            await("HTTP /hello on port " + port)
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(START_TIMEOUT)
                    .ignoreExceptions()
                    .until(() -> {
                        if (!process.isAlive()) {
                            throw new AssertionError("Quarkus exited before serving /hello. Process output:\n"
                                    + readLogs(output));
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
                            try (InputStream input = connection.getInputStream()) {
                                response.set(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                            }
                            return true;
                        } finally {
                            connection.disconnect();
                        }
                    });
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    "Quarkus did not serve /hello on port " + port + ". Process output:\n" + readLogs(output), e);
        }
        return response.get();
    }

    private static void terminate(Process process, Path output) throws IOException, InterruptedException {
        process.destroy();
        if (!process.waitFor(STOP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            if (!process.waitFor(STOP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                throw new AssertionError(
                        "Unable to terminate application process. Process output:\n" + readLogs(output));
            }
        }
        assertThat(process.isAlive()).isFalse();
    }

    private static String readLogs(Path output) throws IOException {
        return Files.exists(output) ? Files.readString(output, StandardCharsets.UTF_8) : "";
    }
}
