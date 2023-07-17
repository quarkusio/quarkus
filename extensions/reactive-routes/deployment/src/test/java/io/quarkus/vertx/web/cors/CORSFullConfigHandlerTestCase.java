package io.quarkus.vertx.web.cors;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.TestRoute;

public class CORSFullConfigHandlerTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestRoute.class)
                    .addAsResource("conf/cors-config-full.properties", "application.properties"));

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
                .header("Access-Control-Allow-Methods", "GET,PUT,POST")
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .header("Access-Control-Allow-Headers", "X-Custom")
                .header("Access-Control-Max-Age", "86400");

        given().header("Origin", "http://custom.origin.quarkus")
                .when()
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Allow-Methods", "GET,PUT,POST")
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .header("Access-Control-Allow-Headers", "X-Custom");

        given().header("Origin", "http://www.quarkus.io")
                .header("Access-Control-Request-Method", "PUT")
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://www.quarkus.io")
                .header("Access-Control-Allow-Methods", "GET,PUT,POST")
                .header("Access-Control-Expose-Headers", "Content-Disposition");

    }

    @Test
    @DisplayName("Returns only allowed headers and methods")
    public void corsPartialMethodsTestServlet() {
        given().header("Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Request-Method", "DELETE")
                .header("Access-Control-Request-Headers", "X-Custom,X-Custom2")
                .when()
                .options("/test").then()
                .statusCode(200)
                .log().headers()
                .header("Access-Control-Allow-Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Allow-Methods", "GET,PUT,POST") // Should not return DELETE
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .header("Access-Control-Allow-Headers", "X-Custom");// Should not return X-Custom2
    }

}
