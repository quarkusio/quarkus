package io.quarkus.rest.client.reactive.stork;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.quarkus.rest.client.reactive.stork.HelloResource.HELLO_WORLD;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.QuarkusDevModeTest;

public class StorkDevModeTest {

    public static final String WIREMOCK_RESPONSE = "response from the wiremock server";

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void setUp() {
        wireMockServer = new WireMockServer(options().port(8766));
        wireMockServer.stubFor(WireMock.get("/hello")
                .willReturn(aResponse().withFixedDelay(1000)
                        .withBody(WIREMOCK_RESPONSE).withStatus(200)));
        wireMockServer.start();
    }

    @AfterAll
    public static void shutDown() {
        wireMockServer.stop();
    }

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PassThroughResource.class, HelloResource.class, HelloClient.class)
                    .addAsResource(
                            new File("src/test/resources/stork-dev-application.properties"),
                            "application.properties"));

    @Test
    void shouldModifyStorkSettings() {
        // @formatter:off
        when()
                .get("/helper")
        .then()
                .statusCode(200)
                .body(equalTo(HELLO_WORLD));
        // @formatter:on

        TEST.modifyResourceFile("application.properties",
                v -> v.replaceAll("stork.hello-service.service-discovery.1=.*",
                        "stork.hello-service.service-discovery.1=localhost:8766"));
        // @formatter:off
        when()
                .get("/helper")
        .then()
                .statusCode(200)
                .body(equalTo(WIREMOCK_RESPONSE));
        // @formatter:on
    }
}
