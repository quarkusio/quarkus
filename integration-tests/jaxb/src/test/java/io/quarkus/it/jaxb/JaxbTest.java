package io.quarkus.it.jaxb;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JaxbTest {

    @Test
    public void book() {
        RestAssured.given().when()
                .param("name", "Foundation")
                .get("/jaxb/book")
                .then()
                .statusCode(200)
                .body(is(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><book><title>Foundation</title></book>"));
    }
}
