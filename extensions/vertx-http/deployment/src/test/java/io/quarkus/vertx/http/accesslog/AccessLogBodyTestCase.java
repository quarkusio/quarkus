package io.quarkus.vertx.http.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
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

    @BeforeEach
    public void before() throws IOException {
        Files.createDirectories(logDirectory);
    }

    @AfterEach
    public void after() throws IOException {
        IoUtils.recursiveDelete(logDirectory);
    }

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
                    try (Stream<Path> files = Files.list(logDirectory)) {
                        assertThat(files.count()).isEqualTo(1);
                    }
                    String data = Files.readString(logDirectory.resolve("server.log"));
                    assertThat(data).contains("request=hello-request");
                    assertThat(data).contains("response=echo:hello-request");
                });
    }

    @ApplicationScoped
    public static class EchoRoute {
        public void register(@Observes Router router) {
            router.post("/echo").handler(rc -> rc.response().end("echo:" + rc.body().asString()));
        }
    }
}
