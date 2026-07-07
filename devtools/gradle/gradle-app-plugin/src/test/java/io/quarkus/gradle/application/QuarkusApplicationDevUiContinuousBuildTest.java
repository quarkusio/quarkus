package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class QuarkusApplicationDevUiContinuousBuildTest extends QuarkusApplicationContinuousBuildTestSupport {

    private static final Pattern CONTINUOUS_TESTING_DEV_UI_URL = Pattern
            .compile("Continuous Testing Dev UI: (https?://\\S+/q/dev-ui/continuous-testing)");
    private static final Pattern MAIN_LISTENER_URL = Pattern
            .compile("Listening on: (https?://(?:\\[[^\\]\\s]+]|[A-Za-z0-9._~-]+):[0-9]+)");
    private static final Pattern MANAGEMENT_LISTENER_URL = Pattern
            .compile("Management interface listening on (https?://(?:\\[[^\\]\\s]+]|[A-Za-z0-9._~-]+):[0-9]+)");

    @Test
    void devUiControlsTheGradleOwnedContinuousTestingSession() throws Exception {
        writeContinuousTestApplication();
        enableContinuousTestingDevUi();

        Path receipt = testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-iteration.properties"));
        Path testSource = testProjectDir.resolve("src/test/java/org/acme/GreetingServiceTest.java");

        try (var build = startContinuousBuild("quarkusApplicationDev", "--no-quarkus-debug")) {
            build.await("initial dev-mode continuous-test run", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true")
                            && occurrences(build.stdout(), "Tests completed at") >= 1
                            && build.stdout().contains("Listening on:"));

            writePassingGreetingTest(testSource, "discover Dev UI URL");
            build.await("first lifecycle Dev UI URL", RELOAD_TIMEOUT,
                    () -> occurrences(build.stdout(), "Tests completed at") >= 2
                            && continuousTestingDevUiUrl(build.stdout()).isPresent());

            URI devUiUrl = continuousTestingDevUiUrl(build.stdout()).orElseThrow();
            URI mainUrl = mainListenerUrl(build.stdout()).orElseThrow();
            URI managementUrl = managementListenerUrl(build.stdout())
                    .orElseThrow(() -> new AssertionError("Management listener missing from output:\n" + build.stdout()));
            assertThat(devUiUrl.resolve("/"))
                    .isEqualTo(mainUrl.resolve("/"))
                    .isNotEqualTo(managementUrl.resolve("/"));
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<Void> page = httpClient.send(
                    HttpRequest.newBuilder(devUiUrl).timeout(Duration.ofSeconds(10)).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(page.statusCode()).isEqualTo(200);

            assertJsonRpcBoolean(httpClient, devUiUrl, "stop", true);
            int runsBeforeStart = occurrences(build.stdout(), "Tests completed at");
            assertJsonRpcBoolean(httpClient, devUiUrl, "start", true);
            build.await("test run after Dev UI start", RELOAD_TIMEOUT,
                    () -> occurrences(build.stdout(), "Tests completed at") > runsBeforeStart);

            int runsBeforeRunAll = occurrences(build.stdout(), "Tests completed at");
            assertJsonRpcBoolean(httpClient, devUiUrl, "runAll", true);
            int expectedRuns = runsBeforeRunAll + 1;
            build.await("test run requested through Dev UI", RELOAD_TIMEOUT,
                    () -> occurrences(build.stdout(), "Tests completed at") >= expectedRuns);
            assertJsonRpcBoolean(httpClient, devUiUrl, "runFailed", true);
            assertThat(executeJsonRpc(httpClient, devUiUrl, "getStatus")).containsPattern("\"object\"\\s*:\\s*\\{");

            writePassingGreetingTest(testSource, "repeat Dev UI URL");
            build.await("repeated lifecycle Dev UI URL", RELOAD_TIMEOUT,
                    () -> occurrences(build.stdout(), "Continuous Testing Dev UI: " + devUiUrl) >= 2);
        }
    }

    @Test
    void reEnablingLiveReloadReplaysGradleOutputsWithoutAnotherEdit() throws Exception {
        ReplayApplication application = writeLiveReloadReplayApplication();
        Path greeting = application.greeting();
        Path receipt = application.receipt();
        Path trigger = application.trigger();
        try (var build = startContinuousBuild("quarkusApplicationDev", "--no-quarkus-debug")) {
            build.await("initial live-reload replay application", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true")
                            && mainListenerUrl(build.stdout()).isPresent());
            writePassingGreetingTest(application.testSource(), "discover replay Dev UI URL");
            build.await("live-reload replay Dev UI URL", RELOAD_TIMEOUT,
                    () -> continuousTestingDevUiUrl(build.stdout()).isPresent());
            URI devUiUrl = continuousTestingDevUiUrl(build.stdout()).orElseThrow();
            URI versionUrl = mainListenerUrl(build.stdout()).orElseThrow().resolve("/version");
            HttpClient httpClient = HttpClient.newHttpClient();
            assertThat(httpGet(httpClient, versionUrl)).isEqualTo("initial");

            assertLiveReloadState(httpClient, devUiUrl, false);
            long initialSequence = receiptSequence(receipt);
            writeVersionedGreetingService(greeting, "first-paused");
            build.await("first disabled live-reload delivery", RELOAD_TIMEOUT,
                    () -> receiptSequence(receipt) > initialSequence
                            && fileContains(receipt, "outcome=PENDING,SENT_LIVE_RELOAD_DISABLED"));
            assertThat(httpGet(httpClient, versionUrl)).isEqualTo("initial");

            long firstPausedSequence = receiptSequence(receipt);
            writeVersionedGreetingService(greeting, "second-paused");
            build.await("coalesced disabled live-reload delivery", RELOAD_TIMEOUT,
                    () -> receiptSequence(receipt) > firstPausedSequence
                            && fileContains(receipt, "outcome=PENDING,SENT_LIVE_RELOAD_DISABLED"));
            assertThat(httpGet(httpClient, versionUrl)).isEqualTo("initial");

            long secondPausedSequence = receiptSequence(receipt);
            assertLiveReloadState(httpClient, devUiUrl, true);
            build.await("triggered replay after re-enabling live reload", RELOAD_TIMEOUT,
                    () -> receiptSequence(receipt) > secondPausedSequence
                            && fileContains(trigger, "generation=2")
                            && httpBodyEquals(httpClient, versionUrl, "second-paused"));
            assertThat(httpGet(httpClient, versionUrl)).isEqualTo("second-paused");
        }
    }

    @Test
    void reEnablingLiveReloadReplaysCompactRebaselineWithoutAnotherEdit() throws Exception {
        ReplayApplication application = writeLiveReloadReplayApplication();
        Path receipt = application.receipt();
        Path trigger = application.trigger();
        try (var build = startContinuousBuild(
                "quarkusApplicationDev",
                "-D" + DELTA_MAX_BYTES_PROPERTY + "=1",
                "--no-quarkus-debug")) {
            build.await("initial compact replay application", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true")
                            && mainListenerUrl(build.stdout()).isPresent());
            writePassingGreetingTest(application.testSource(), "discover compact replay Dev UI URL");
            build.await("compact replay Dev UI URL", RELOAD_TIMEOUT,
                    () -> continuousTestingDevUiUrl(build.stdout()).isPresent());
            URI devUiUrl = continuousTestingDevUiUrl(build.stdout()).orElseThrow();
            URI versionUrl = mainListenerUrl(build.stdout()).orElseThrow().resolve("/version");
            HttpClient httpClient = HttpClient.newHttpClient();
            assertThat(httpGet(httpClient, versionUrl)).isEqualTo("initial");

            assertLiveReloadState(httpClient, devUiUrl, false);
            long pausedSequence = receiptSequence(receipt);
            int initialRebaselines = occurrences(build.stdout(), REBASELINE_LOG);
            writeVersionedGreetingService(application.greeting(), "paused-rebaseline");
            build.await("disabled compact rebaseline delivery", RELOAD_TIMEOUT,
                    () -> receiptSequence(receipt) > pausedSequence
                            && fileContains(receipt, "outcome=PENDING,SENT_LIVE_RELOAD_DISABLED"));
            assertThat(httpGet(httpClient, versionUrl)).isEqualTo("initial");

            long disabledSequence = receiptSequence(receipt);
            assertLiveReloadState(httpClient, devUiUrl, true);
            build.await("triggered compact rebaseline replay", RELOAD_TIMEOUT,
                    () -> receiptSequence(receipt) > disabledSequence
                            && fileContains(trigger, "generation=2")
                            && occurrences(build.stdout(), REBASELINE_LOG) > initialRebaselines
                            && httpBodyEquals(httpClient, versionUrl, "paused-rebaseline"));
            assertThat(httpGet(httpClient, versionUrl)).isEqualTo("paused-rebaseline");
        }
    }

    private ReplayApplication writeLiveReloadReplayApplication() throws IOException {
        writeContinuousTestApplication();
        enableContinuousTestingDevUi();
        Files.writeString(testProjectDir.resolve("build.gradle"), """

                dependencies {
                    implementation "io.quarkus:quarkus-rest"
                }
                """, StandardOpenOption.APPEND);
        Path greeting = testProjectDir.resolve("src/main/java/org/acme/GreetingService.java");
        writeVersionedGreetingService(greeting, "initial");
        Files.writeString(greeting.getParent().resolve("VersionResource.java"), """
                package org.acme;

                import jakarta.inject.Inject;
                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                @Path("/version")
                public class VersionResource {
                    @Inject
                    GreetingService greetingService;

                    @GET
                    public String version() {
                        return greetingService.version();
                    }
                }
                """);
        return new ReplayApplication(
                greeting,
                testProjectDir.resolve(Path.of("build", "quarkus-dev", "dev-iteration.properties")),
                testProjectDir.resolve(Path.of("build", "quarkus-dev", "live-reload-replay.trigger")),
                testProjectDir.resolve("src/test/java/org/acme/GreetingServiceTest.java"));
    }

    private void enableContinuousTestingDevUi() throws IOException {
        Files.writeString(testProjectDir.resolve("build.gradle"), """

                dependencies {
                    implementation "io.quarkus:quarkus-vertx-http"
                    implementation "io.quarkus:quarkus-smallrye-health"
                }

                quarkusApplication {
                    dev {
                        continuousTesting = true
                    }
                }
                """, StandardOpenOption.APPEND);
        Path resources = Files.createDirectories(testProjectDir.resolve("src/main/resources"));
        Files.writeString(resources.resolve("application.properties"), """
                quarkus.http.port=0
                quarkus.management.enabled=true
                quarkus.management.port=0
                """);
    }

    private static Optional<URI> continuousTestingDevUiUrl(String output) {
        var matcher = CONTINUOUS_TESTING_DEV_UI_URL.matcher(output);
        return matcher.find() ? Optional.of(URI.create(matcher.group(1))) : Optional.empty();
    }

    private static Optional<URI> mainListenerUrl(String output) {
        var matcher = MAIN_LISTENER_URL.matcher(output);
        return matcher.find() ? Optional.of(URI.create(matcher.group(1))) : Optional.empty();
    }

    private static Optional<URI> managementListenerUrl(String output) {
        var matcher = MANAGEMENT_LISTENER_URL.matcher(output);
        return matcher.find() ? Optional.of(URI.create(matcher.group(1))) : Optional.empty();
    }

    private static void assertJsonRpcBoolean(HttpClient httpClient, URI devUiUrl, String method, boolean expected)
            throws Exception {
        assertThat(executeJsonRpc(httpClient, devUiUrl, "devui-continuous-testing", method))
                .containsPattern("\"object\"\\s*:\\s*" + expected);
    }

    private static String executeJsonRpc(HttpClient httpClient, URI devUiUrl, String method) throws Exception {
        return executeJsonRpc(httpClient, devUiUrl, "devui-continuous-testing", method);
    }

    private static String executeJsonRpc(HttpClient httpClient, URI devUiUrl, String namespace, String method)
            throws Exception {
        URI webSocketUri = new URI(
                devUiUrl.getScheme().equals("https") ? "wss" : "ws",
                null,
                devUiUrl.getHost(),
                devUiUrl.getPort(),
                "/q/dev-ui/json-rpc-ws",
                null,
                null);
        var response = new CompletableFuture<String>();
        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(webSocketUri, new WebSocket.Listener() {
                    private final StringBuilder payload = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        payload.append(data);
                        if (last) {
                            response.complete(payload.toString());
                        } else {
                            webSocket.request(1);
                        }
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        response.completeExceptionally(error);
                    }
                })
                .get(10, TimeUnit.SECONDS);
        try {
            webSocket.sendText("""
                    {"jsonrpc":"2.0","id":1,"method":"%s_%s","params":{}}
                    """.formatted(namespace, method), true).get(10, TimeUnit.SECONDS);
            return response.get(10, TimeUnit.SECONDS);
        } finally {
            webSocket.abort();
        }
    }

    private static void assertLiveReloadState(HttpClient httpClient, URI devUiUrl, boolean expected) throws Exception {
        assertThat(executeJsonRpc(httpClient, devUiUrl, "devui-logstream", "toggleLiveReload"))
                .containsPattern("\"liveReloadEnabled\"\\s*:\\s*" + expected);
    }

    private static String httpGet(HttpClient httpClient, URI uri) throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private static boolean httpBodyEquals(HttpClient httpClient, URI uri, String expected) {
        try {
            return expected.equals(httpGet(httpClient, uri));
        } catch (Exception ignored) {
            return false;
        }
    }

    private record ReplayApplication(Path greeting, Path receipt, Path trigger, Path testSource) {
    }

    private static void writeVersionedGreetingService(Path source, String version) throws IOException {
        Files.writeString(source, """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class GreetingService {
                    public String hello() {
                        return "hello";
                    }

                    public String version() {
                        return "%s";
                    }
                }
                """.formatted(version));
    }

}
