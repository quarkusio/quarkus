package io.quarkus.it.keycloak;

import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
public class AnnotationBasedTenantTest {

    @Test
    public void testJaxRsHttpSecurityPolicyNoRbac() {
        String token = getTokenWithRole("role1");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/tenant-echo/jax-rs-perm-check")
                .then().statusCode(200)
                .body(Matchers.equalTo(("tenant-id=tenant-public-key, static.tenant.id=tenant-public-key, name=alice")));

        token = getTokenWithRole("wrong-role");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/tenant-echo/jax-rs-perm-check")
                .then().statusCode(403);
    }

    @Test
    public void testJaxRsHttpSecurityPolicyWithRbac() {
        String token = getTokenWithRole("role1");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/tenant-echo2/jax-rs-perm-check")
                .then().statusCode(200)
                .body(Matchers.equalTo(("tenant-id=tenant-public-key, static.tenant.id=tenant-public-key, name=alice")));

        token = getTokenWithRole("wrong-role");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/tenant-echo2/jax-rs-perm-check")
                .then().statusCode(403);
    }

    @Test
    public void testClassicHttpSecurityPolicyWithRbac() {
        // authentication fails as tenant id is not selected when non-JAX-RS permission check is performed
        String token = getTokenWithRole("role1");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/tenant-echo2/classic-perm-check")
                .then().statusCode(401);
    }

    @Test
    public void testJaxRsAndClassicHttpSecurityPolicyNoRbac() {
        // authentication fails as tenant id is not selected when non-JAX-RS permission check is performed
        RestAssured.given().auth().oauth2(getTokenWithRole("role3", "role2", "role1"))
                .when().get("/api/tenant-echo/classic-and-jaxrs-perm-check")
                .then().statusCode(401);
    }

    @Test
    public void testJaxRsAndClassicHttpSecurityPolicyWithRbac() {
        // authentication fails as tenant id is not selected when non-JAX-RS permission check is performed
        RestAssured.given().auth().oauth2(getTokenWithRole("role2", "role1"))
                .when().get("/api/tenant-echo2/classic-and-jaxrs-perm-check")
                .then().statusCode(401);
    }

    @Test
    public void testJaxRsIdentityAugmentation() {
        // pass JAX-RS permission check but missing permission
        String token = getTokenWithRole("role2");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/tenant-echo/hr-identity-augmentation")
                .then().statusCode(403);

        token = getTokenWithRole("role3");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/tenant-echo/hr-identity-augmentation")
                .then().statusCode(200)
                .body(Matchers.equalTo(("tenant-id=tenant-public-key, static.tenant.id=tenant-public-key, name=alice")));

        // test mapped role can be used by a roles policy
        token = getTokenWithRole("role4");
        RestAssured.given().auth().oauth2(token)
                .when().get("/api/tenant-echo/hr-identity-augmentation")
                .then().statusCode(200)
                .body(Matchers.equalTo(("tenant-id=tenant-public-key, static.tenant.id=tenant-public-key, name=alice")));
    }

    static String getTokenWithRole(String... roles) {
        return Jwt.claim("scope", "read:data").preferredUserName("alice").groups(Set.of(roles)).sign();
    }
}
