package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.sun.net.httpserver.HttpServer;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

public class QuarkusProdModeTestConfusingLogTest {

    @RegisterExtension
    static final QuarkusProdModeTest simpleApp = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClass(Main.class))
            .setApplicationName("simple-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true);

    static HttpClient client;

    @BeforeAll
    static void setUp() {
        // No tear down, because there's no way to shut down the client explicitly before Java 21 :(
        // We'll just hope no connection is left hanging.
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();
    }

    @Test
    public void shouldWaitForAppActuallyStarted() {
        thenAppIsRunning();

        whenStopApp();
        thenAppIsNotRunning();

        whenStartApp();
        thenAppIsRunning();
    }

    private void whenStopApp() {
        simpleApp.stop();
    }

    private void whenStartApp() {
        simpleApp.start();
    }

    private void thenAppIsNotRunning() {
        assertNotNull(simpleApp.getExitCode(), "App is running");
        assertThrows(IOException.class, this::tryReachApp, "App's HTTP server is still running");
    }

    private void thenAppIsRunning() {
        assertNull(simpleApp.getExitCode(), "App is not running");
        assertDoesNotThrow(this::tryReachApp, "App's HTTP server is not reachable");
    }

    private void tryReachApp() throws IOException, InterruptedException {
        String response = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:8081/test")).GET().build(),
                HttpResponse.BodyHandlers.ofString())
                .body();
        // If the app is reachable, this is the expected response.
        assertEquals("OK", response, "App returned unexpected response");
    }

    @QuarkusMain
    public static class Main {
        public static void main(String[] args) {
            // Use an unrelated log to trick QuarkusProdModeTest into thinking the app started
            System.out.println(
                    "HHH000511: The -9999.-9999.-9999 version for [org.hibernate.dialect.PostgreSQLDialect] is no longer supported, hence certain features may not work properly. The minimum supported version is 12.0.0. Check the community dialects project for available legacy versions.");
            try {
                // Delay the actual app start so there's a decent chance of QuarkusProdModeTest
                // being ahead of the app -- otherwise we wouldn't reproduce the bug.
                Thread.sleep(500);
                // Expose an endpoint proving the app is up
                HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
                server.createContext("/test", exchange -> {
                    String response = "OK";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                });
                server.start();
                Quarkus.run(args);
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
