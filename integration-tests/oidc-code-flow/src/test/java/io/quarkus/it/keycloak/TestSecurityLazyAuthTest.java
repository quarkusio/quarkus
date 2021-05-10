package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.TestOidcSecurity;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(ProtectedResource.class)
public class TestSecurityLazyAuthTest {

    @Test
    @TestOidcSecurity(user = "user1", roles = "viewer")
    public void testWithDummyUser() {
        RestAssured.when().get("test-security").then()
                .body(is("user1"));
    }

    @Test
    @TestOidcSecurity(user = "userOidc", roles = "viewer", claims = {
            @Claim(key = "email", value = "user@gmail.com")
    })
    public void testJwtWithDummyUser() {
        RestAssured.when().get("test-security-oidc").then()
                .body(is("userOidc:viewer:user@gmail.com"));
    }

}
