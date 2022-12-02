package com.example.reactivejackson;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(SimpleEndpoint.class)
class SimpleEndpointTest {

    @Test
    public void ensure_there_is_no_null_attribute() {
        when().get("person")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(
                        "name", is("Foo"),
                        "size()", is(1));
    }

}
