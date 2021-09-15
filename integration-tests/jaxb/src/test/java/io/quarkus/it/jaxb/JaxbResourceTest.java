package io.quarkus.it.jaxb;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JaxbResourceTest {

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

    @Test
    public void shouldReturnCustomer() {
        RestAssured.given().when()
                .get("/jaxb/customer")
                .then()
                .statusCode(200)
                .body(is("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<customer id=\"1\">\n" +
                        "    <age>18</age>\n" +
                        "    <name>fake-name</name>\n" +
                        "</customer>\n"));
    }
}
