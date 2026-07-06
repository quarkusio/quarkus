package io.quarkus.vertx.http.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class AccessLogBodyLargePayloadTestCase {

    private static final long ONE_GIGA = 1024L * 1024L * 1024L;
    private static final long STREAMED_BODY_SIZE = 20L * 1024L * 1024L;
    private static final int MEDIUM_BODY_SIZE = 8 * 1024;

    @RegisterExtension
    public static QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    try {
                        Path logDirectory = Files.createTempDirectory("quarkus-access-log-body-large-tests");
                        Properties p = new Properties();
                        p.setProperty("quarkus.http.access-log.enabled", "true");
                        p.setProperty("quarkus.http.access-log.log-to-file", "true");
                        p.setProperty("quarkus.http.access-log.base-file-name", "server");
                        p.setProperty("quarkus.http.access-log.log-directory", logDirectory.toAbsolutePath().toString());
                        p.setProperty("quarkus.http.access-log.pattern",
                                "%r %s request=%{REQUEST_BODY} response=%{RESPONSE_BODY}");
                        p.setProperty("quarkus.http.access-log.log-request-body", "true");
                        p.setProperty("quarkus.http.access-log.log-response-body", "true");
                        p.setProperty("quarkus.http.access-log.max-logged-body-size", "10");
                        p.setProperty("quarkus.http.limits.max-body-size", "2G");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        p.store(out, null);

                        return ShrinkWrap.create(JavaArchive.class)
                                .add(new ByteArrayAsset(out.toByteArray()), "application.properties")
                                .addClasses(LargePayloadRoute.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

    @TestHTTPResource
    String url;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "quarkus.http.access-log.log-directory")
    Path logDirectory;

    @Test
    public void testStreamedRequestBodyIsLoggedWithoutBuffering() throws Exception {
        Path bodyFile = Files.createTempFile("access-log-streamed-request", ".bin");
        try {
            try (RandomAccessFile file = new RandomAccessFile(bodyFile.toFile(), "rw")) {
                file.setLength(STREAMED_BODY_SIZE);
            }

            HttpResponse<String> response = httpClient().send(HttpRequest.newBuilder()
                    .uri(uri("large-header"))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofFile(bodyFile))
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(String.valueOf(STREAMED_BODY_SIZE));

            Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                    .atMost(30, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        String line = logLineForPath("/large-header");
                        assertThat(line).contains("request=<" + STREAMED_BODY_SIZE + " bytes, body too large to log>");
                        assertThat(line).contains("response=" + STREAMED_BODY_SIZE);
                    });
        } finally {
            Files.deleteIfExists(bodyFile);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "test-large-files", matches = "true")
    public void testHugeRequestBodyIsLoggedWithoutBuffering() throws Exception {
        Path bodyFile = Files.createTempFile("access-log-huge-request", ".bin");
        try {
            try (RandomAccessFile file = new RandomAccessFile(bodyFile.toFile(), "rw")) {
                file.setLength(ONE_GIGA);
            }

            HttpResponse<String> response = httpClient().send(HttpRequest.newBuilder()
                    .uri(uri("large-header"))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofFile(bodyFile))
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(String.valueOf(ONE_GIGA));

            Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                    .atMost(120, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        String line = logLineForPath("/large-header");
                        assertThat(line).contains("request=<" + ONE_GIGA + " bytes, body too large to log>");
                        assertThat(line).contains("response=" + ONE_GIGA);
                    });
        } finally {
            Files.deleteIfExists(bodyFile);
        }
    }

    @Test
    public void testMediumRequestBodyIsTruncatedInLogAndEndpointStillReceivesFullBody() throws IOException {
        String body = "A".repeat(MEDIUM_BODY_SIZE);

        String response = RestAssured.given()
                .body(body)
                .contentType("text/plain")
                .post("/large-echo")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertThat(response).isEqualTo("length:" + MEDIUM_BODY_SIZE);

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String line = logLineForPath("/large-echo");
                    assertThat(line).contains("request=AAAAAAAAAA...(truncated)");
                    assertThat(line).contains("response=length:819...(truncated)");
                });
    }

    @Test
    @EnabledIfSystemProperty(named = "test-large-files", matches = "true")
    public void testHugeResponseBodyIsLoggedWithoutBuffering() throws Exception {
        HttpResponse<InputStream> response = httpClient().send(HttpRequest.newBuilder()
                .uri(uri("huge-response"))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofInputStream());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(drain(response.body())).isEqualTo(ONE_GIGA);

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String line = logLineForPath("/huge-response");
                    assertThat(line).contains("response=<non-buffer body>");
                });
    }

    private URI uri(String path) {
        String base = url.endsWith("/") ? url : url + "/";
        return URI.create(base + path);
    }

    private static HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private static long drain(InputStream inputStream) throws IOException {
        try (inputStream) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
            }
            return total;
        }
    }

    private String logLineForPath(String path) throws IOException {
        return Files.readString(logDirectory.resolve("server.log")).lines()
                .filter(line -> line.contains(path))
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    @ApplicationScoped
    public static class LargePayloadRoute {

        private File hugeResponseFile;

        public void register(@Observes Router router) throws IOException {
            hugeResponseFile = File.createTempFile("access-log-huge-response", ".bin");
            hugeResponseFile.deleteOnExit();
            try (RandomAccessFile file = new RandomAccessFile(hugeResponseFile, "rw")) {
                file.setLength(ONE_GIGA);
            }

            router.post("/large-header").handler(rc -> rc.response()
                    .end(rc.request().getHeader("Content-Length")));
            router.post("/large-echo").handler(rc -> {
                int length = rc.body().buffer().length();
                rc.response().end("length:" + length);
            });
            router.get("/huge-response").handler(rc -> rc.response().sendFile(hugeResponseFile.getAbsolutePath()));
        }
    }
}
