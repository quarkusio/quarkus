package io.quarkus.vertx.http.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;

/**
 * The {@code %{CONNECTION_STATUS}} attribute of the access log tells whether the client closed the connection before
 * the response was completed.
 */
public class AccessLogConnectionStatusTestCase {

    private static final Path LOG_DIRECTORY = createLogDirectory();

    @RegisterExtension
    public static QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Routes.class))
            .overrideRuntimeConfigKey("quarkus.http.access-log.enabled", "true")
            .overrideRuntimeConfigKey("quarkus.http.access-log.log-to-file", "true")
            .overrideRuntimeConfigKey("quarkus.http.access-log.base-file-name", "server")
            .overrideRuntimeConfigKey("quarkus.http.access-log.log-directory", LOG_DIRECTORY.toAbsolutePath().toString())
            .overrideRuntimeConfigKey("quarkus.http.access-log.pattern", "%r %{CONNECTION_STATUS}");

    private static Path createLogDirectory() {
        try {
            return Files.createTempDirectory("quarkus-tests");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "quarkus.http.access-log.log-directory")
    Path logDirectory;

    @BeforeEach
    public void before() throws IOException {
        Files.createDirectories(logDirectory);
    }

    @AfterEach
    public void after() throws IOException {
        IoUtils.recursiveDelete(logDirectory);
    }

    @Test
    public void connectionStatusTellsWhetherTheClientWentAway() throws Exception {
        HttpClient client = vertx.createHttpClient();
        try {
            HttpClientRequest request = client.request(HttpMethod.GET, RestAssured.port, "localhost", "/slow").await();
            request.send();
            Thread.sleep(200);
            request.connection().close().await();
        } finally {
            client.close().await();
        }
        RestAssured.get("/fast").then().statusCode(200);

        Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String data = Files.readString(logDirectory.resolve("server.log"));
                    assertThat(data).contains("GET /slow HTTP/1.1 X");
                    assertThat(data).contains("GET /fast HTTP/1.1 -");
                });
    }

    public static class Routes {

        void init(@Observes Router router) {
            router.get("/fast").handler(rc -> rc.response().end("fast"));
            router.get("/slow").handler(rc -> rc.vertx().setTimer(2000, ignored -> rc.response().end("slow")));
        }
    }
}
