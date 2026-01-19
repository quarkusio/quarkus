package io.quarkus.it.vertx;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.restassured.http.Header;

@QuarkusTest
@QuarkusTestResource(UseRestClientEndpointTest.WireMockExtension.class)
public class UseRestClientEndpointTest {

    @Test
    void shouldPropagate() {
        given()
                .header(new Header("X-Flow-Instance-Id", "JJJJJ"))
                .header(new Header("X-Flow-Task-Ids", "great-value, another-value, some-value"))
                .get("/use-rest-client/greeting")
                .then()
                .body(Matchers.containsString("hello"));
    }

    public static class ConfigureRestClient implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.simple.url", "http://localhost:8888",
                    "org.eclipse.microprofile.rest.client.propagateHeaders", "X-Flow-Instance-Id,X-Flow-Task-Ids");
        }
    }

    public static class WireMockExtension implements QuarkusTestResourceLifecycleManager {
        private WireMockServer wireMockServer;

        @Override
        public Map<String, String> start() {
            wireMockServer = new WireMockServer();
            wireMockServer.start();

            wireMockServer.stubFor(get(urlEqualTo("/only-get"))
                    .withHeader("X-Flow-Instance-Id", equalTo("JJJJJ"))
                    .withHeader("X-Flow-Task-Ids", containing("great-value"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "text/plain")
                            .withBody(
                                    "hello")));

            return Map.of("quarkus.rest-client.simple.url", wireMockServer.baseUrl(),
                    "org.eclipse.microprofile.rest.client.propagateHeaders", "X-Flow-Instance-Id,X-Flow-Task-Ids");
        }

        @Override
        public void stop() {
            if (null != wireMockServer) {
                wireMockServer.stop();
            }
        }
    }
}
