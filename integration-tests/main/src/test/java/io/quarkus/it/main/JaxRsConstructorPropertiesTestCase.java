package io.quarkus.it.main;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSender;

@QuarkusTest
public class JaxRsConstructorPropertiesTestCase {

    @Test
    public void testReturnedDirectly() {
        when().get("/constructorproperties/direct").then()
                .body("name", is("direct"),
                        "value", is("directvalue"));
    }

    @Test
    public void testConvertedWithJsonbAndReturnedAsString() {
        when().get("/constructorproperties/jsonb").then()
                .body("name", is("jsonb"),
                        "value", is("jsonbvalue"));
    }

    @Test
    public void testWrappedInResponse() {
        when().get("/constructorproperties/response").then()
                .body("name", is("response"),
                        "value", is("responsevalue"));
    }

    @Test
    public void testWrappedInServerSentEventMessage() {
        when().get("/constructorproperties/sse").then().body(containsString("ssevalue"));
    }

    private static RequestSender when() {
        return RestAssured.when();
    }
}
