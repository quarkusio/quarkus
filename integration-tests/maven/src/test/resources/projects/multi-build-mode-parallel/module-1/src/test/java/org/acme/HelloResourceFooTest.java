package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@Tag("foo")
@QuarkusTest
public class HelloResourceFooTest {
    @Test
    void testHelloEndpoint() throws Exception {
        for (int i = 0; i < 5; i++) {
            if (i > 0) {
                Thread.sleep(1_000L); // Make it wait to cause build conflicts
            }
            given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello foo 1"));
        }
    }
}
