package io.quarkus.deployment.dev.remotedev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.runtime.util.HashUtil;

class HttpRemoteDevPackageClientTest {

    private static final String SESSION_HEADER = "X-Quarkus-Session";

    @TempDir
    Path directory;

    @Test
    void requestsSessionOwnedReconnectAfterUploadingApplicationModel() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger probes = new AtomicInteger();
        AtomicInteger uploads = new AtomicInteger();
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, connects.get() == 0 ? "lib/deployment/appmodel.dat" : "");
                case "/probe" -> {
                    probes.incrementAndGet();
                    respond(exchange, 200, "");
                }
                case "/lib/deployment/appmodel.dat" -> {
                    uploads.incrementAndGet();
                    respond(exchange, 200, "");
                }
                default -> respond(exchange, 404, "");
            }
        });
        Path appModel = write("lib/deployment/appmodel.dat", "model");

        try (var client = client(server)) {
            RemoteDevPackageClientResult connected = client.connect(Map.of("lib/deployment/appmodel.dat", "sha1"));
            assertThat(connected.requestedPaths()).containsExactly("lib/deployment/appmodel.dat");

            RemoteDevPackageClientResult result = client.send(new RemoteDevPackageDiff(
                    List.of(new RemoteDevPackageChange("lib/deployment/appmodel.dat", appModel, sha1(appModel),
                            Files.size(appModel))),
                    List.of()));

            assertThat(result.outcome()).isEqualTo(RemoteDevPackageClientOutcome.RECONNECT_REQUIRED);
            assertThat(connects).hasValue(1);
            assertThat(probes.get()).isGreaterThanOrEqualTo(1);
            assertThat(uploads).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void defersDeletionsUntilAfterApplicationModelRestartAndReconnect() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = server(exchange -> {
            String path = exchange.getRequestURI().getPath();
            switch (path) {
                case "/connect" -> connect(exchange, connects, "");
                case "/probe" -> respond(exchange, 200, "");
                case "/lib/main/removed.jar" -> {
                    requests.add(exchange.getRequestMethod() + " " + path);
                    respond(exchange, connects.get() == 2 ? 200 : 423, "");
                }
                default -> {
                    requests.add(exchange.getRequestMethod() + " " + path);
                    respond(exchange, 200, "");
                }
            }
        });
        Path application = write("app/application.jar", "application");
        Path appModel = write("lib/deployment/appmodel.dat", "model");

        try (var client = client(server)) {
            client.connect(Map.of());
            RemoteDevPackageClientResult sent = client.send(new RemoteDevPackageDiff(
                    List.of(
                            new RemoteDevPackageChange("lib/deployment/appmodel.dat", appModel, sha1(appModel),
                                    Files.size(appModel)),
                            new RemoteDevPackageChange("app/application.jar", application, sha1(application),
                                    Files.size(application))),
                    List.of("lib/main/removed.jar")));

            assertThat(sent.outcome()).isEqualTo(RemoteDevPackageClientOutcome.RECONNECT_REQUIRED);
            assertThat(requests).containsExactly(
                    "PUT /app/application.jar",
                    "PUT /lib/deployment/appmodel.dat");

            RemoteDevPackageClientResult reconnected = client.connect(Map.of());

            assertThat(reconnected.outcome()).isEqualTo(RemoteDevPackageClientOutcome.CONNECTED);
            assertThat(connects).hasValue(2);
            assertThat(requests).containsExactly(
                    "PUT /app/application.jar",
                    "PUT /lib/deployment/appmodel.dat",
                    "DELETE /lib/main/removed.jar");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void serializesChangePollingAndPackageDelivery() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        CountDownLatch pollStarted = new CountDownLatch(1);
        CountDownLatch releasePoll = new CountDownLatch(1);
        CountDownLatch uploadReceived = new CountDownLatch(1);
        ExecutorService serverExecutor = Executors.newCachedThreadPool();
        ExecutorService deliveryExecutor = Executors.newSingleThreadExecutor();
        HttpServer server = server(serverExecutor, exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/dev" -> {
                    pollStarted.countDown();
                    boolean released;
                    try {
                        released = releasePoll.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        released = false;
                    }
                    if (!released) {
                        respond(exchange, 500, "");
                    } else {
                        respond(exchange, 204, "");
                    }
                }
                case "/app/application.jar" -> {
                    uploadReceived.countDown();
                    respond(exchange, 200, "");
                }
                default -> respond(exchange, 404, "");
            }
        });
        Path application = write("app/application.jar", "application");

        try (var client = client(server)) {
            client.connect(Map.of());
            client.startChangePolling(() -> {
            });
            assertThat(pollStarted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<RemoteDevPackageClientResult> delivery = deliveryExecutor.submit(() -> client.send(
                    new RemoteDevPackageDiff(
                            List.of(new RemoteDevPackageChange("app/application.jar", application, sha1(application),
                                    Files.size(application))),
                            List.of())));
            boolean uploadOverlappedPolling;
            try {
                uploadOverlappedPolling = uploadReceived.await(1, TimeUnit.SECONDS);
            } finally {
                releasePoll.countDown();
            }

            assertThat(delivery.get(5, TimeUnit.SECONDS).changed()).isEqualTo(1);
            assertThat(uploadOverlappedPolling).isFalse();
        } finally {
            releasePoll.countDown();
            server.stop(0);
            deliveryExecutor.shutdownNow();
            serverExecutor.shutdownNow();
        }
    }

    @Test
    void reportsFilesRejectedWithAStaleSession() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger uploads = new AtomicInteger();
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/app/application.jar" -> respond(exchange, uploads.incrementAndGet() == 1 ? 203 : 200, "");
                default -> respond(exchange, 404, "");
            }
        });
        Path application = write("app/application.jar", "application");

        try (var client = client(server)) {
            client.connect(Map.of("app/application.jar", "sha1"));

            RemoteDevPackageClientResult result = client.send(new RemoteDevPackageDiff(
                    List.of(new RemoteDevPackageChange("app/application.jar", application, sha1(application),
                            Files.size(application))),
                    List.of()));

            assertThat(result.outcome()).isEqualTo(RemoteDevPackageClientOutcome.RECONNECT_REQUIRED);
            assertThat(connects).hasValue(1);
            assertThat(uploads).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsStaleSessionWithoutTryingToUploadAnUnchangedRequestedFile() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger changedUploads = new AtomicInteger();
        AtomicInteger unchangedUploads = new AtomicInteger();
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, connects.get() == 0 ? "" : "app/unchanged.jar");
                case "/app/changed.jar" -> {
                    changedUploads.incrementAndGet();
                    respond(exchange, 203, "");
                }
                case "/app/unchanged.jar" -> {
                    unchangedUploads.incrementAndGet();
                    respond(exchange, 200, "");
                }
                default -> respond(exchange, 404, "");
            }
        });
        Path changed = write("app/changed.jar", "changed");

        try (var client = client(server)) {
            client.connect(Map.of(
                    "app/changed.jar", "changed-sha1",
                    "app/unchanged.jar", "unchanged-sha1"));

            RemoteDevPackageClientResult result = client.send(new RemoteDevPackageDiff(
                    List.of(new RemoteDevPackageChange("app/changed.jar", changed, sha1(changed),
                            Files.size(changed))),
                    List.of()));

            assertThat(result.outcome()).isEqualTo(RemoteDevPackageClientOutcome.RECONNECT_REQUIRED);
            assertThat(connects).hasValue(1);
            assertThat(changedUploads).hasValue(1);
            assertThat(unchangedUploads).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void pollReconnectListenerRunsOutsideRequestLockAndPollingResumesAfterConnect() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger polls = new AtomicInteger();
        CountDownLatch secondPoll = new CountDownLatch(1);
        ExecutorService reconnectExecutor = Executors.newSingleThreadExecutor();
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/dev" -> {
                    if (polls.incrementAndGet() == 1) {
                        respond(exchange, 203, "");
                    } else {
                        secondPoll.countDown();
                        respond(exchange, 204, "");
                    }
                }
                default -> respond(exchange, 404, "");
            }
        });

        try (var client = client(server)) {
            client.connect(Map.of());
            client.startChangePolling(() -> {
                try {
                    reconnectExecutor.submit(() -> client.connect(Map.of())).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IOException("Unable to reconnect from listener", e);
                }
            });

            assertThat(secondPoll.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(connects).hasValue(2);
        } finally {
            server.stop(0);
            reconnectExecutor.shutdownNow();
        }
        assertThat(remoteDevPollingThreads()).isEmpty();
    }

    @Test
    void retriesFailedReconnectNotificationAndPausesUntilReconnect() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger polls = new AtomicInteger();
        AtomicInteger notifications = new AtomicInteger();
        CountDownLatch notificationSucceeded = new CountDownLatch(1);
        CountDownLatch secondPoll = new CountDownLatch(1);
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/dev" -> {
                    if (polls.incrementAndGet() == 1) {
                        respond(exchange, 203, "");
                    } else {
                        secondPoll.countDown();
                        respond(exchange, 204, "");
                    }
                }
                default -> respond(exchange, 404, "");
            }
        });

        try (var client = fastClient(server)) {
            client.connect(Map.of());
            client.startChangePolling(() -> {
                if (notifications.incrementAndGet() == 1) {
                    throw new IOException("simulated trigger publication failure");
                }
                notificationSucceeded.countDown();
            });

            assertThat(notificationSucceeded.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(polls).hasValue(1);
            client.connect(Map.of());
            assertThat(secondPoll.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(notifications).hasValue(2);
        } finally {
            server.stop(0);
        }
        assertThat(remoteDevPollingThreads()).isEmpty();
    }

    @Test
    void closeStopsPollerPausedForReconnect() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        CountDownLatch notified = new CountDownLatch(1);
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/dev" -> respond(exchange, 203, "");
                default -> respond(exchange, 404, "");
            }
        });

        HttpRemoteDevPackageClient client = client(server);
        try {
            client.connect(Map.of());
            client.startChangePolling(notified::countDown);
            assertThat(notified.await(5, TimeUnit.SECONDS)).isTrue();

            client.close();
        } finally {
            client.close();
            server.stop(0);
        }
        assertThat(remoteDevPollingThreads()).isEmpty();
    }

    @Test
    void closeInterruptsAndJoinsPollerBlockedInHttpRequest() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        CountDownLatch pollStarted = new CountDownLatch(1);
        CountDownLatch releasePoll = new CountDownLatch(1);
        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        ExecutorService closeExecutor = Executors.newSingleThreadExecutor();
        HttpServer server = server(serverExecutor, exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/dev" -> {
                    pollStarted.countDown();
                    try {
                        releasePoll.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    respond(exchange, 204, "");
                }
                default -> respond(exchange, 404, "");
            }
        });
        HttpRemoteDevPackageClient client = fastClient(server);

        try {
            client.connect(Map.of());
            client.startChangePolling(() -> {
            });
            assertThat(pollStarted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> close = closeExecutor.submit(() -> {
                client.close();
                return null;
            });
            assertThat(close.get(2, TimeUnit.SECONDS)).isNull();
            assertThat(remoteDevPollingThreads()).isEmpty();
        } finally {
            releasePoll.countDown();
            client.close();
            server.stop(0);
            closeExecutor.shutdownNow();
            serverExecutor.shutdownNow();
        }
    }

    @Test
    void rejectsPackageFileDriftBeforeUpload() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger uploads = new AtomicInteger();
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/app/application.jar" -> {
                    uploads.incrementAndGet();
                    respond(exchange, 200, "");
                }
                default -> respond(exchange, 404, "");
            }
        });
        Path application = write("app/application.jar", "one");
        long capturedSize = Files.size(application);
        String capturedHash = sha1(application);

        try (var client = client(server)) {
            client.connect(Map.of());
            Files.writeString(application, "two");
            assertThatThrownBy(() -> client.send(new RemoteDevPackageDiff(
                    List.of(new RemoteDevPackageChange("app/application.jar", application, capturedHash, capturedSize)),
                    List.of())))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("changed after snapshot capture");

            Files.writeString(application, "different-size");
            assertThatThrownBy(() -> client.send(new RemoteDevPackageDiff(
                    List.of(new RemoteDevPackageChange("app/application.jar", application, capturedHash, capturedSize)),
                    List.of())))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("changed after snapshot capture");
            assertThat(uploads).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsAFileReplacedByASymlinkBeforeUpload() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger uploads = new AtomicInteger();
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/app/application.jar" -> {
                    uploads.incrementAndGet();
                    respond(exchange, 200, "");
                }
                default -> respond(exchange, 404, "");
            }
        });
        Path application = write("app/application.jar", "outside content");
        RemoteDevPackageDiff diff = RemoteDevPackageSnapshot.capture(directory)
                .diffSince(RemoteDevPackageSnapshot.empty(), directory);
        Path outside = directory.resolve("outside.jar");
        Files.writeString(outside, "outside content");

        try (var client = client(server)) {
            client.connect(Map.of());
            Files.delete(application);
            createSymbolicLinkOrAbort(application, outside);

            assertThatThrownBy(() -> client.send(diff))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("changed after snapshot capture");
            assertThat(uploads).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsAFileReplacedByADirectoryBeforeUpload() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger uploads = new AtomicInteger();
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, "");
                case "/app/application.jar" -> {
                    uploads.incrementAndGet();
                    respond(exchange, 200, "");
                }
                default -> respond(exchange, 404, "");
            }
        });
        Path application = write("app/application.jar", "application");
        RemoteDevPackageDiff diff = RemoteDevPackageSnapshot.capture(directory)
                .diffSince(RemoteDevPackageSnapshot.empty(), directory);

        try (var client = client(server)) {
            client.connect(Map.of());
            Files.delete(application);
            Files.createDirectory(application);

            assertThatThrownBy(() -> client.send(diff))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("changed after snapshot capture");
            assertThat(uploads).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    private static void createSymbolicLinkOrAbort(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.abort("Symbolic links are not supported by this test environment: " + e);
        }
    }

    private HttpRemoteDevPackageClient client(HttpServer server) {
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        return new HttpRemoteDevPackageClient(new RemoteDevPackageClientConfig(uri, Optional.of("password")));
    }

    private HttpRemoteDevPackageClient fastClient(HttpServer server) {
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        return new HttpRemoteDevPackageClient(
                new RemoteDevPackageClientConfig(uri, Optional.of("password")),
                Duration.ofSeconds(5),
                Duration.ofMillis(1),
                Duration.ofMillis(1),
                Duration.ofMillis(10),
                Duration.ofSeconds(1));
    }

    private static HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        return server(null, handler);
    }

    private static HttpServer server(ExecutorService executor, com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        if (executor != null) {
            server.setExecutor(executor);
        }
        server.createContext("/", handler);
        server.start();
        return server;
    }

    private static void connect(HttpExchange exchange, AtomicInteger connects, String requested) throws IOException {
        exchange.getResponseHeaders().add(SESSION_HEADER, "session-" + connects.incrementAndGet());
        respond(exchange, 200, requested);
    }

    private static void respond(HttpExchange exchange, int status, String content) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] body = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private Path write(String relativePath, String content) throws IOException {
        Path file = directory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private static String sha1(Path file) throws IOException {
        return HashUtil.sha1(Files.readAllBytes(file));
    }

    private static List<Thread> remoteDevPollingThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(thread -> thread.getName().equals("Quarkus remote dev change poller"))
                .toList();
    }
}
