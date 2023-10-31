package org.acme.googlecloudfunctions;

import io.quarkus.google.cloud.functions.test.FunctionType;
import io.quarkus.google.cloud.functions.test.WithFunction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@WithFunction(FunctionType.HTTP)
class HelloWorldHttpFunctionTest {
    @Test
    void testService() {
        when()
                .get()
                .then()
                .statusCode(200)
                .body(is("Hello World"));
    }
}