package io.quarkus.xmlsecurity.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class XmlsecurityResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/xmlsecurity")
                .then()
                .statusCode(200)
                .body(is("Hello xmlsecurity"));
    }
}
