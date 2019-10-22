package io.quarkus.reproducer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.reproducer.jacksonbuilder.model.ModelWithSerializerAndDeserializerOnField;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ModelWithSerializerAndDeserializerOnFieldResourceTest {

    @Test
    public void testSerializer() throws IOException {
        given()
                .contentType("application/json")
                .when().get("/fieldserder/tester/whatever")
                .then()
                .statusCode(200)
                .body("name", equalTo("tester"))
                .body("inner.someValue", equalTo("unchangeable"));
    }

    @Test
    public void testDeserializer() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        given()
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(
                        new ModelWithSerializerAndDeserializerOnField("tester",
                                new ModelWithSerializerAndDeserializerOnField.Inner())))
                .when().post("/fieldserder")
                .then()
                .statusCode(200)
                .body(is("tester/immutable"));
    }
}
