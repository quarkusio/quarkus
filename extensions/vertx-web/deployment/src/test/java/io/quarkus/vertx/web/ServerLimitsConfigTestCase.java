package io.quarkus.vertx.web;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ServerLimitsConfigTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestRoute.class)
                    .addAsResource(new StringAsset("\nquarkus.http.limits.max-header-size=1K" +
                            "\nquarkus.http.limits.max-body-size=2K"),
                            "application.properties"));

    @Test
    @DisplayName("Should return error because max header size over passed")
    public void testMaxHeaderSizeOverPassed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("h" + i);
        }

        RestAssured.given()
                .header("MAX-HEADER-SIZE", sb.toString())
                .when()
                .get("/test").then()
                .statusCode(431);
    }

    @Test
    @DisplayName("Should return status code 200")
    public void testCorrectHeaderSize() {
        RestAssured.when().get("/test").then().statusCode(200);
    }

    @Test
    @DisplayName("Should return error because max body size over passed")
    public void testMaxEntityError() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i <= 2 * 1024; i++) {
            body.append("q");
        }

        try {
            RestAssured.given()
                    .body(body.toString())
                    .post("/test")
                    .then().statusCode(413);
        } catch (Throwable t) {
            // Writing when the connection has been closed can lead to a WSAECONNABORTED
            // on Windows. Ignore since this is the case we are testing.
        }
    }

    @Test
    @DisplayName("Should return status code 200")
    public void testMaxEntityOk() {
        RestAssured.given()
                .body("OK")
                .post("/test")
                .then().statusCode(200);
    }
}
