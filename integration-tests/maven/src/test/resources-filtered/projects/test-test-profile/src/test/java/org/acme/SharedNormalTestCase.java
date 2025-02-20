package org.acme;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SharedNormalTestCase {

    @Test
    public void basicInjectedService() {
        RestAssured.when()
                .get("/app/greeting/Ducky")
                .then()
                .statusCode(200)
                .body(is("Hello Ducky"));
    }
}
