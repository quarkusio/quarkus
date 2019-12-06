package io.quarkus.it.jackson;

import static io.quarkus.it.jackson.TestUtil.getObjectMapperForTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.jackson.model.RegisteredPojoModel;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RegisteredPojoModelResourceTest {

    @Test
    public void testSimplePojoModel() throws IOException {
        RegisteredPojoModel model = new RegisteredPojoModel();
        model.setId("123");
        model.setVersion(3);
        model.setValue("some");

        given()
                .contentType("application/json")
                .body(model.toJson(getObjectMapperForTest()))
                .when().post("/registeredpojomodel")
                .then()
                .statusCode(201)
                .body("id", equalTo("123"))
                .body("version", equalTo(3))
                .body("value", equalTo("some"));
    }
}
