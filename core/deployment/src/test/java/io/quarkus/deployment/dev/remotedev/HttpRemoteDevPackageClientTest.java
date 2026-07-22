package io.quarkus.deployment.dev.remotedev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class HttpRemoteDevPackageClientTest {

    private static final String SESSION_HEADER = "X-Quarkus-Session";

    @TempDir
    Path directory;

    @Test
    void reconnectsAfterUploadingApplicationModel() throws Exception {
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

            client.send(new RemoteDevPackageDiff(
                    List.of(new RemoteDevPackageChange("lib/deployment/appmodel.dat", appModel, "sha1", Files.size(appModel))),
                    List.of()));

            assertThat(connects).hasValue(2);
            assertThat(probes.get()).isGreaterThanOrEqualTo(1);
            assertThat(uploads).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sendsApplicationModelAfterOtherChangesAndDeletions() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = server(exchange -> {
            String path = exchange.getRequestURI().getPath();
            switch (path) {
                case "/connect" -> connect(exchange, connects, "");
                case "/probe" -> respond(exchange, 200, "");
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
            client.send(new RemoteDevPackageDiff(
                    List.of(
                            new RemoteDevPackageChange("lib/deployment/appmodel.dat", appModel, "model-sha1",
                                    Files.size(appModel)),
                            new RemoteDevPackageChange("app/application.jar", application, "application-sha1",
                                    Files.size(application))),
                    List.of("lib/main/removed.jar")));

            assertThat(requests).containsExactly(
                    "PUT /app/application.jar",
                    "DELETE /lib/main/removed.jar",
                    "PUT /lib/deployment/appmodel.dat");
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
            client.startChangePolling();
            assertThat(pollStarted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<RemoteDevPackageClientResult> delivery = deliveryExecutor.submit(() -> client.send(
                    new RemoteDevPackageDiff(
                            List.of(new RemoteDevPackageChange("app/application.jar", application, "sha1",
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
    void reconnectsAndRetriesFilesRejectedWithAStaleSession() throws Exception {
        AtomicInteger connects = new AtomicInteger();
        AtomicInteger uploads = new AtomicInteger();
        HttpServer server = server(exchange -> {
            switch (exchange.getRequestURI().getPath()) {
                case "/connect" -> connect(exchange, connects, connects.get() == 0 ? "" : "app/application.jar");
                case "/app/application.jar" -> respond(exchange, uploads.incrementAndGet() == 1 ? 203 : 200, "");
                default -> respond(exchange, 404, "");
            }
        });
        Path application = write("app/application.jar", "application");

        try (var client = client(server)) {
            client.connect(Map.of("app/application.jar", "sha1"));

            RemoteDevPackageClientResult result = client.send(new RemoteDevPackageDiff(
                    List.of(new RemoteDevPackageChange("app/application.jar", application, "sha1",
                            Files.size(application))),
                    List.of()));

            assertThat(result.changed()).isEqualTo(1);
            assertThat(connects).hasValue(2);
            assertThat(uploads).hasValue(2);
        } finally {
            server.stop(0);
        }
    }

    private HttpRemoteDevPackageClient client(HttpServer server) {
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        return new HttpRemoteDevPackageClient(new RemoteDevPackageClientConfig(uri, Optional.of("password")));
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
}
