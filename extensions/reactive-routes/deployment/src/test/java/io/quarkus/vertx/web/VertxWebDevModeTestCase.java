package io.quarkus.vertx.web;

import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class VertxWebDevModeTestCase {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DevModeRoute.class));

    @Test
    public void testRunningInDevMode() {
        RestAssured.given()
                .body("OK")
                .post("/test")
                .then().statusCode(200)
                .body(Matchers.equalTo("test route"));

        runner.modifySourceFile(DevModeRoute.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("test route", "new code");
            }
        });
        for (int i = 0; i < 10; ++i) {
            RestAssured.given()
                    .body("OK")
                    .post("/test")
                    .then().statusCode(200)
                    .body(Matchers.equalTo("new code"));
        }
        RestAssured.given()
                .get("/assert")
                .then().statusCode(200)
                .body(Matchers.equalTo("OK"));
    }

}
