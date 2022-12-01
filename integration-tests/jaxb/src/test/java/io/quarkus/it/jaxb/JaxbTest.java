package io.quarkus.it.jaxb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class JaxbTest {

    @Test
    public void book() {
        given().when()
                .param("name", "Foundation")
                .get("/jaxb/book")
                .then()
                .statusCode(200)
                .body(is(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><book><title>Foundation</title></book>"));
    }

    @Test
    public void seeAlso() {
        given()
                .when().get("/jaxb/see-also")
                .then()
                .statusCode(200)
                .contentType(ContentType.XML)
                .log().ifValidationFails()
                .body("response.evenMoreZeep", is(equalTo("ZEEEP")));
    }

}
