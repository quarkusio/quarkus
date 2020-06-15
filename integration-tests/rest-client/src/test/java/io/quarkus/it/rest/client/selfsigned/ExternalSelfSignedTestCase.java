package io.quarkus.it.rest.client.selfsigned;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ExternalSelfSignedTestCase {

    @Test
    public void included() {
        RestAssured.when()
                .get("/self-signed")
                .then()
                .statusCode(200)
                .body(is(not(empty())));
    }
}
