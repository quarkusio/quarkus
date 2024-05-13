package io.quarkus.it.openapi.security;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(TestSecurityResource.class)
public class TestSecurityReactiveRoutesTest {

    @TestSecurity(user = "Martin", roles = "admin")
    @Test
    public void testSecurityWithReactiveRoutesAndQuarkusRest() {
        RestAssured.get("reactive-routes")
                .then()
                .statusCode(200)
                .header("reactive-routes-filter", is("true"))
                .body(is("Martin"));
    }

}
