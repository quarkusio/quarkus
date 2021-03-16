package io.quarkus.it.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ModelWithJsonNamingStrategyResourceTest {

    @Test
    public void testStrategy() throws IOException {
        given()
                .when().get("/json-naming/")
                .then()
                .statusCode(200)
                .body(containsString("blog_title"));
    }

}
