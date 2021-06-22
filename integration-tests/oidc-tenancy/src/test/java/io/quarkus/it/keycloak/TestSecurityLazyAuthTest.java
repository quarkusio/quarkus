package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.TokenIntrospection;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(TenantOpaqueResource.class)
public class TestSecurityLazyAuthTest {

    @Test
    @TestSecurity(user = "alice", roles = "user")
    public void testWithDummyUser() {
        RestAssured.when().get("tenant-oidc/api/testsecurity").then()
                .body(is("tenant-oidc-opaque:alice"));
    }

    @Test
    @TestSecurity(user = "alice", roles = "user")
    @OidcSecurity(introspectionRequired = true, introspection = {
            @TokenIntrospection(key = "email", value = "user@gmail.com")
    })
    public void testOpaqueTokenWithDummyUser() {
        RestAssured.when().get("tenant-oidc/api/user").then()
                .body(is("tenant-oidc-opaque:alice:user:user@gmail.com"));
    }

}
