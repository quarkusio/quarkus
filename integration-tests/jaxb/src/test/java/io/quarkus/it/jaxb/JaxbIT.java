package io.quarkus.it.jaxb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class JaxbIT extends JaxbTest {
    //We have to test native executable of Jaxb
    @Test
    public void bookWithParent() {
        given().when()
                .param("name", "Foundation")
                .param("iban", "4242")
                .get("/jaxb/bookwithparent")
                .then()
                .statusCode(200)
                .body(is(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><bookWithParent><IBAN>4242</IBAN><title>Foundation</title></bookWithParent>"));
    }
}
