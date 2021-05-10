package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.TestJwtSecurity;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(ProtectedJwtResource.class)
public class TestSecurityLazyAuthTest {

    @Test
    @TestJwtSecurity(user = "user1", roles = "viewer")
    public void testWithDummyUser() {
        RestAssured.when().get("test-security").then()
                .body(is("user1"));
    }

    @Test
    @TestJwtSecurity(user = "userJwt", roles = "viewer", claims = {
            @Claim(key = "email", value = "user@gmail.com")
    })
    public void testJwtWithDummyUser() {
        RestAssured.when().get("test-security-jwt").then()
                .body(is("userJwt:viewer:user@gmail.com"));
    }

}
