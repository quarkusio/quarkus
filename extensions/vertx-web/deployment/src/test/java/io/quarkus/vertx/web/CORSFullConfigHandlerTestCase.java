package io.quarkus.vertx.web;

import static io.restassured.RestAssured.given;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CORSFullConfigHandlerTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestRoute.class)
                    .addAsResource("cors-config-full.properties", "application.properties"));

    @Test
    @DisplayName("Handles a detailed CORS config request correctly")
    public void corsFullConfigTestServlet() {
        given().header("Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "X-Custom")
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Allow-Methods", "GET")
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .header("Access-Control-Allow-Headers", "X-Custom");

        given().header("Origin", "http://www.quarkus.io")
                .header("Access-Control-Request-Method", "POST,PUT")
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://www.quarkus.io")
                .header("Access-Control-Allow-Methods", "POST,PUT")
                .header("Access-Control-Expose-Headers", "Content-Disposition");
    }

    @Test
    @DisplayName("Returns only allowed headers and methods")
    public void corsPartialMethodsTestServlet() {
        given().header("Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Request-Method", "GET,DELETE")
                .header("Access-Control-Request-Headers", "X-Custom,X-Custom2")
                .when()
                .options("/test").then()
                .statusCode(200)
                .log().headers()
                .header("Access-Control-Allow-Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Allow-Methods", "GET") // Should not return DELETE
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .header("Access-Control-Allow-Headers", "X-Custom");// Should not return X-Custom2
    }

}
