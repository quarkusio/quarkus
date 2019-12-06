package io.quarkus.it.jackson;

import static io.quarkus.it.jackson.TestUtil.getObjectMapperForTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.jackson.model.ModelWithBuilder;
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
                .body(model.toJson(getObjectMapperForTest()))
                .when().post("/modelwithbuilder")
                .then()
                .statusCode(201)
                .body("id", equalTo("123"))
                .body("version", equalTo(3))
                .body("value", equalTo("some"));
    }
}
