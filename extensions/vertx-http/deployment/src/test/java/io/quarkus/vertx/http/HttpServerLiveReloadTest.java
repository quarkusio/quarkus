package io.quarkus.vertx.http;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

class HttpServerLiveReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(HttpServerLiveReloadRoutes.class));

    @Test
    void serverValuesSurviveLiveReload() {
        RestAssured.get("/port").then().statusCode(200).body(is("8080"));
        RestAssured.get("/call-myself").then().statusCode(200).body(is("hello"));

        // Renaming the route makes dev mode restart the application, which keeps the HTTP server.
        TEST.modifySourceFile(HttpServerLiveReloadRoutes.class, s -> s.replace("\"/port\"", "\"/port-after-reload\""));

        RestAssured.get("/port-after-reload").then().statusCode(200).body(is("8080"));
        RestAssured.get("/call-myself").then().statusCode(200).body(is("hello"));
        RestAssured.get("/port-at-router-setup").then().statusCode(200).body(is("8080"));
    }
}
