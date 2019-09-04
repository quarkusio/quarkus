package io.quarkus.reproducer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.reproducer.jacksonbuilder.model.ModelWithBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ModelWithBuilderResourceTest {

    @Test
    public void testModelWithBuilder() throws IOException {
        ModelWithBuilder model = new ModelWithBuilder.Builder("123")
                .withVersion(3)
                .withValue("some")
                .build();

        given()
                .contentType("application/json")
                .body(model.toJson())
                .when().post("/modelwithbuilder")
                .then()
                .statusCode(201)
                .body("id", equalTo("123"))
                .body("version", equalTo(3))
                .body("value", equalTo("some"));
    }
}
