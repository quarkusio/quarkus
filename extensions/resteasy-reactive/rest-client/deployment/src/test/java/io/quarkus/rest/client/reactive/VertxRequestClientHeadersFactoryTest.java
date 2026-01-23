package io.quarkus.rest.client.reactive;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.net.ServerSocket;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.restassured.http.Header;
import io.smallrye.mutiny.Uni;

public class VertxRequestClientHeadersFactoryTest {

    private static final int port = 8922;

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(SimpleRestClient.class, SimpleVertxRoute.class,
                    VertxRequestClientHeadersFactory.class))
            .overrideConfigKey("org.eclipse.microprofile.rest.client.propagateHeaders", "X-Flow-Instance-Id,X-Flow-Task-Ids")
            .overrideConfigKey("quarkus.rest-client.simple.url", "http://localhost:" + port);

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void configureWireMock() {

        wireMockServer = new WireMockServer(port);
        wireMockServer.start();
        wireMockServer.stubFor(WireMock.get("/sayHello")
                .withHeader("X-Flow-Instance-Id", equalTo("JJJJJ"))
                .withHeader("X-Flow-Task-Ids", containing("great-value"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(
                                "hello")));
    }

    @AfterAll
    public static void shutDownWireMock() {
        wireMockServer.stop();
    }

    @Test
    void shouldPropagate() {
        given()
                .header(new Header("X-Flow-Instance-Id", "JJJJJ"))
                .header(new Header("X-Flow-Task-Ids", "great-value, another-value, some-value"))
                .get("/use-rest-client/greeting")
                .then()
                .body(Matchers.containsString("hello"));
    }

    public static int getRandomFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    @RegisterRestClient(configKey = "simple")
    @RegisterClientHeaders(VertxRequestClientHeadersFactory.class)
    interface SimpleRestClient {

        @GET
        @Path("/sayHello")
        Uni<String> sayHello();
    }

    @RouteBase(path = "/use-rest-client")
    public static class SimpleVertxRoute {

        @Inject
        @RestClient
        SimpleRestClient call;

        @Route(path = "/greeting", methods = Route.HttpMethod.GET)
        public Uni<String> greeting() {
            return call.sayHello();
        }

    }
}
