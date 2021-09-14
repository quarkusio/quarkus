package io.quarkus.it.main;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SwaggerUITestCase {

    @Test
    public void testSwaggerUi() {
        RestAssured.when().get("/q/swagger-ui").then()
                .body(containsString("#swagger-ui"))
                .body(containsString("/openapi"));
    }

}
