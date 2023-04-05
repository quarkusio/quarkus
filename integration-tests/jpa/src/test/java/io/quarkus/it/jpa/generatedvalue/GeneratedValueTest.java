package io.quarkus.it.jpa.generatedvalue;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GeneratedValueTest {

    @Test
    public void test() {
        when().get("/jpa-test/generated-value/test").then()
                .body(is("OK"))
                .statusCode(200);
    }

}
