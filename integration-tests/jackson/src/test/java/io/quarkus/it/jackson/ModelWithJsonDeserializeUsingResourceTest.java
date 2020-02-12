package io.quarkus.it.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.it.jackson.model.ModelWithSerializerAndDeserializerOnField;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ModelWithJsonDeserializeUsingResourceTest {

    @Test
    public void testDeserializer() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        given()
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(
                        new ModelWithSerializerAndDeserializerOnField("tester",
                                new ModelWithSerializerAndDeserializerOnField.Inner())))
                .when().post("/deserializerUsing")
                .then()
                .statusCode(200)
                .body(is("constant"));
    }

}
