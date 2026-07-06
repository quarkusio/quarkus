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
import io.vertx.ext.web.Router;

public class AccessLogBodyExplicitOptInTestCase {

    @RegisterExtension
    public static QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    try {
                        Path logDirectory = Files.createTempDirectory("quarkus-access-log-body-opt-in-tests");
                        Properties p = new Properties();
                        p.setProperty("quarkus.http.access-log.enabled", "true");
                        p.setProperty("quarkus.http.access-log.log-to-file", "true");
                        p.setProperty("quarkus.http.access-log.base-file-name", "server");
                        p.setProperty("quarkus.http.access-log.log-directory", logDirectory.toAbsolutePath().toString());
                        p.setProperty("quarkus.http.access-log.pattern",
                                "%r %s request=%{REQUEST_BODY} response=%{RESPONSE_BODY}");
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
    public void testPatternTokensDoNotEnableBodyCapture() throws IOException {
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
                    assertThat(data).contains("request=");
                    assertThat(data).contains("response=");
                    assertThat(data).doesNotContain("hello-request");
                    assertThat(data).doesNotContain("echo:hello-request");
                });
    }

    @ApplicationScoped
    public static class EchoRoute {
        public void register(@Observes Router router) {
            router.post("/echo").handler(rc -> rc.response().end("echo:" + rc.body().asString()));
        }
    }
}
