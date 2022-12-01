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
        RegisteredPojoModel parent = new RegisteredPojoModel();
        parent.setId("123");
        parent.setVersion(3);
        parent.setValue("some");

        RegisteredPojoModel child = new RegisteredPojoModel();
        child.setId("234");
        child.setVersion(1);
        child.setValue("value");

        parent.setChild(child);
        child.setParent(parent);

        given()
                .contentType("application/json")
                .body(parent.toJson(getObjectMapperForTest()))
                .when().post("/registeredpojomodel")
                .then()
                .statusCode(201)
                .body("id", equalTo("123"))
                .body("version", equalTo(3))
                .body("value", equalTo("some"))
                .body("parent", equalTo(null))
                .body("child.id", equalTo("234"))
                .body("child.version", equalTo(1))
                .body("child.value", equalTo("value"))
                .body("child.parent", equalTo("123"))
                .body("child.child", equalTo(null));
    }
}
