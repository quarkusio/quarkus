package io.quarkus.it.jpa.attributeconverter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AttributeConverterTest {

    @Test
    public void withCdiExplicitScope() {
        given().queryParam("theData", "MyExpectedReturnedData")
                .when().get("/jpa-test/attribute-converter/with-cdi-explicit-scope").then()
                .body(is("MyExpectedReturnedData"))
                .statusCode(200);
    }

    @Test
    public void withCdiImplicitScope() {
        given().queryParam("theData", "MyExpectedReturnedData")
                .when().get("/jpa-test/attribute-converter/with-cdi-implicit-scope").then()
                .body(is("MyExpectedReturnedData"))
                .statusCode(200);
    }

    @Test
    public void withoutCdi() {
        given().queryParam("theData", "MyExpectedReturnedData")
                .when().get("/jpa-test/attribute-converter/without-cdi").then()
                .body(is("MyExpectedReturnedData"))
                .statusCode(200);
    }

}
