package io.quarkus.vertx.http.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

/**
 * The {@code %{CONNECTION_STATUS}} attribute of the access log tells whether the client closed the connection before
 * the response was completed.
 */
public class AccessLogConnectionStatusTestCase {

    @RegisterExtension
    public static QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(() -> {
                try {
                    Path logDirectory = Files.createTempDirectory("quarkus-tests");
                    Properties p = new Properties();
                    p.setProperty("quarkus.http.access-log.enabled", "true");
                    p.setProperty("quarkus.http.access-log.log-to-file", "true");
                    p.setProperty("quarkus.http.access-log.base-file-name", "server");
                    p.setProperty("quarkus.http.access-log.log-directory", logDirectory.toAbsolutePath().toString());
                    p.setProperty("quarkus.http.access-log.pattern", "%r %{CONNECTION_STATUS}");
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    p.store(out, null);
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Routes.class)
                            .add(new ByteArrayAsset(out.toByteArray()), "application.properties");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

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
        try (Socket socket = new Socket("localhost", RestAssured.port)) {
            socket.getOutputStream().write("GET /slow HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            Thread.sleep(200);
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
