package io.quarkus.vertx.graphql.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocketConnectOptions;

@QuarkusTest
class VertxGraphqlTest {
    public static int getPortFromConfig() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class).orElse(8081);
    }

    private static Vertx vertx;

    @BeforeAll
    public static void initializeVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    public static void closeVertx() {
        vertx.close();
    }

    @Test
    public void testGraphQlQuery() {
        given().contentType(ContentType.JSON).body("{ \"query\" : \"{ hello }\" }")
                .when().post("/graphql")
                .then().log().ifValidationFails().statusCode(200).body("data.hello", is("world"));
    }

    @Test
    public void testWebSocketSubProtocol() throws Exception {
        HttpClient httpClient = vertx.createHttpClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions().setPort(getPortFromConfig())
                .addSubProtocol("graphql-ws").setURI("/graphql");
        CompletableFuture<Boolean> wsFuture = new CompletableFuture<>();
        httpClient.webSocket(options, event -> wsFuture.complete(event.succeeded()));
        Assertions.assertTrue(wsFuture.get(1, TimeUnit.MINUTES));
    }

}
