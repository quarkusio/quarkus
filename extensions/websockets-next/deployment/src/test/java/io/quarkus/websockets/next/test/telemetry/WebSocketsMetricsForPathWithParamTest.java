package io.quarkus.websockets.next.test.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;

public class WebSocketsMetricsForPathWithParamTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("""
                            quarkus.websockets-next.server.metrics.enabled=true
                            quarkus.websockets-next.client.metrics.enabled=true
                            """), "application.properties")
                    .addClasses(WSClient.class, MontyEcho.class))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus-deployment",
                            Version.getVersion())));

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo/monty/and/python")
    URI montyPythonUri;

    @TestHTTPResource("echo/monty/and/java")
    URI montyJavaUri;

    @TestHTTPResource("echo/monty/and/go")
    URI montyGoUri;

    @Test
    void testServerMetricsForPathWithParameter() {
        try (WSClient client = WSClient.create(vertx).connect(montyPythonUri)) {
            assertEquals("monty python", client.sendAndAwaitReply("payload").toString());
        }
        try (WSClient client = WSClient.create(vertx).connect(montyJavaUri)) {
            assertEquals("monty java", client.sendAndAwaitReply("payload").toString());
        }
        try (WSClient client = WSClient.create(vertx).connect(montyGoUri)) {
            assertEquals("monty go", client.sendAndAwaitReply("payload").toString());
        }

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var body = RestAssured.given().get("/q/metrics").then().statusCode(200).extract().asString();
            assertNotNull(body);
            var urisNotUsingPathParams = body
                    .lines()
                    .filter(l -> !l.trim().startsWith("#"))
                    .filter(l -> l.contains("quarkus_websockets_server_"))
                    .filter(l -> !l.contains("uri=\"/echo/:param1/and/:param2\""))
                    .toList();
            if (!urisNotUsingPathParams.isEmpty()) {
                Assertions.fail("Expected URI was '/echo/:param1/and/:param2', but following metrics has different URI: "
                        + urisNotUsingPathParams);
            }
        });
    }

    @WebSocket(path = "/echo/{param1}/and/{param2}")
    public static class MontyEcho {

        @OnTextMessage
        String process(@PathParam String param1, @PathParam String param2, String ignored) {
            return param1 + " " + param2;
        }

    }
}
