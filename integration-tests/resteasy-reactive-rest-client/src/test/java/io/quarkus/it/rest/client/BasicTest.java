package io.quarkus.it.rest.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class BasicTest {

    @TestHTTPResource("/apples")
    String appleUrl;

    @Test
    public void shouldWork() {
        RestAssured.with().body(appleUrl).post("/call-client")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("size()", is(9))
                .body("[0].cultivar", equalTo("cortland"))
                .body("[1].cultivar", equalTo("cortland2"))
                .body("[2].cultivar", equalTo("cortland3"))
                .body("[3].cultivar", equalTo("cortland"))
                .body("[4].cultivar", equalTo("cortland2"))
                .body("[5].cultivar", equalTo("cortland3"))
                .body("[6].cultivar", equalTo("cortland"))
                .body("[7].cultivar", equalTo("cortland2"))
                .body("[8].cultivar", equalTo("cortland3"));
    }
}
