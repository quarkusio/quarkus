package io.quarkus.websockets.next.test.telemetry;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage.BounceClient;
import io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage.BounceEndpoint;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.restassured.RestAssured;

public class MetricsDisabledWebSocketsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(WSClient.class, BounceEndpoint.class, BounceClient.class))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus-deployment",
                            Version.getVersion())));

    @Inject
    WebSocketConnector<BounceClient> bounceClientConnector;

    @TestHTTPResource("/")
    URI baseUri;

    @Test
    public void testEndpointMetricsDisabled() {
        var clientConnection = bounceClientConnector.baseUri(baseUri).connectAndAwait();
        try {
            clientConnection.sendTextAndAwait("Merry Christmas");
            Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                Assertions.assertEquals(1, BounceEndpoint.MESSAGES.size());
                Assertions.assertEquals("Merry Christmas", BounceEndpoint.MESSAGES.get(0));
                Assertions.assertEquals(1, BounceClient.MESSAGES.size());
                Assertions.assertEquals("Merry Christmas", BounceClient.MESSAGES.get(0));
                RestAssured
                        .given()
                        .get("/q/metrics")
                        .then()
                        .statusCode(200)
                        .body(Matchers.not(Matchers.containsString("quarkus_websockets_server")))
                        .body(Matchers.not(Matchers.containsString("quarkus_websockets_client")));
            });
        } finally {
            clientConnection.closeAndAwait();
        }
    }
}
