package io.quarkus.it.main;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.GreetingEndpoint;
import io.quarkus.it.rest.TestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(TestResource.class)
public class TestHTTPEndpointNestedTestCase {

    @Nested
    class InheritedEndpoint {

        @Test
        void shouldUseOuterClassEndpoint() {
            when().get().then()
                    .statusCode(200)
                    .body(is("TEST"));
        }
    }

    @Nested
    @TestHTTPEndpoint(GreetingEndpoint.class)
    class OverriddenEndpoint {

        @Test
        void shouldUseOwnEndpoint() {
            when().get("/Stuart").then()
                    .statusCode(200)
                    .body(is("Hello Stuart"));
        }
    }
}
