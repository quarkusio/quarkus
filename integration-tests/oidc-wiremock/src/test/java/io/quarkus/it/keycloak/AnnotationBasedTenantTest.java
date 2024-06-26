package io.quarkus.it.keycloak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.quarkus.vertx.http.runtime.security.AbstractPathMatchingHttpSecurityPolicy;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@TestProfile(AnnotationBasedTenantTest.NoProactiveAuthTestProfile.class)
@WithTestResource(value = OidcWiremockTestResource.class, restrictToAnnotatedClass = false)
public class AnnotationBasedTenantTest {
    public static class NoProactiveAuthTestProfile implements QuarkusTestProfile {
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(Map.entry("quarkus.http.auth.proactive", "false"),
                    Map.entry("quarkus.oidc.hr.authentication.user-info-required", "false"),
                    Map.entry("quarkus.oidc.hr.auth-server-url", "http://localhost:8180/auth/realms/quarkus2/"),
                    Map.entry("quarkus.oidc.hr.client-id", "quarkus-app"),
                    Map.entry("quarkus.oidc.hr.credentials.secret", "secret"),
                    Map.entry("quarkus.oidc.hr.tenant-paths", "/api/tenant-echo/http-security-policy-applies-all-same"),
                    Map.entry("quarkus.oidc.hr.token.audience", "http://hr.service"),
                    Map.entry("quarkus.http.auth.policy.roles1.roles-allowed", "role1"),
                    Map.entry("quarkus.http.auth.policy.roles2.roles-allowed", "role2"),
                    Map.entry("quarkus.http.auth.policy.roles3.roles-allowed", "role3,role2"),
                    Map.entry("quarkus.http.auth.policy.roles3.permissions.role3", "get-tenant"),
                    Map.entry("quarkus.http.auth.roles-mapping.role4", "role3"),
                    Map.entry("quarkus.http.auth.permission.jax-rs1.paths", "/api/tenant-echo2/hr-jax-rs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.jax-rs1.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.jax-rs1.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.jax-rs2.paths", "/api/tenant-echo/hr-jax-rs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.jax-rs2.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.jax-rs2.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.classic.paths", "/api/tenant-echo2/hr-classic-perm-check"),
                    Map.entry("quarkus.http.auth.permission.classic.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.combined-part1.paths",
                            "/api/tenant-echo2/hr-classic-and-jaxrs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.combined-part1.policy", "roles2"),
                    Map.entry("quarkus.http.auth.permission.combined-part2.paths",
                            "/api/tenant-echo2/hr-classic-and-jaxrs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.combined-part2.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.combined-part2.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.combined-part3.paths",
                            "/api/tenant-echo/hr-classic-and-jaxrs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.combined-part3.policy", "roles2"),
                    Map.entry("quarkus.http.auth.permission.combined-part4.paths",
                            "/api/tenant-echo/hr-classic-and-jaxrs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.combined-part4.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.combined-part4.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.identity-augmentation.paths",
                            "/api/tenant-echo/hr-identity-augmentation"),
                    Map.entry("quarkus.http.auth.permission.identity-augmentation.policy", "roles3"),
                    Map.entry("quarkus.http.auth.permission.identity-augmentation.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.tenant-annotation-applies-all.paths",
                            "/api/tenant-echo/http-security-policy-applies-all-diff,/api/tenant-echo/http-security-policy-applies-all-same"),
                    Map.entry("quarkus.http.auth.permission.tenant-annotation-applies-all.policy", "admin-role"),
                    Map.entry("quarkus.http.auth.policy.admin-role.roles-allowed", "admin"));
        }

        @Override
        public String getConfigProfile() {
            return "jax-rs-http-perms-test";
        }
    }

    @Test
    public void testClassLevelAnnotation() {
        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo")
                    .then().statusCode(401);

            token = Jwt.preferredUserName("alice")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));

        } finally {
            server.stop();
        }
    }

    @Test
    public void testMethodLevelAnnotation() {
        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // ANNOTATED ENDPOINT
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr")
                    .then().statusCode(401);

            token = Jwt.preferredUserName("alice")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));

            // UNANNOTATED ENDPOINT
            token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/default")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=null, static.tenant.id=null, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=null")));
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsHttpSecurityPolicyNoRbac() {
        // there is one HTTP permission check for this path, and it is executed after @Tenant has chosen right tenant
        // no RBAC annotation is applied
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-jax-rs-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-jax-rs-perm-check")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));

            token = getTokenWithRole("wrong-role");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-jax-rs-perm-check")
                    .then().statusCode(403);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsHttpSecurityPolicyWithRbac() {
        // there is one HTTP permission check for this path, and it is executed after @Tenant has chosen right tenant
        // also the endpoint is secured with @Authenticated
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-jax-rs-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-jax-rs-perm-check")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));

            token = getTokenWithRole("wrong-role");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-jax-rs-perm-check")
                    .then().statusCode(403);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testClassicHttpSecurityPolicyWithRbac() {
        // there is one HTTP permission check for this path, and it is executed before @Tenant comes into action
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-perm-check")
                    .then().statusCode(403);

            // Static tenant id is wrong as authentication happened before tenant were selected via @Tenant
            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("wrong-role");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-perm-check")
                    .then().statusCode(403);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsAndClassicHttpSecurityPolicyNoRbac() {
        // there are 2 HTTP Permission checks for this path, one happens before tenant selection, one happens "after" it
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(403);

            // permission check "combined-part1" as "role2" is missing
            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(403);

            // permission check "combined-part2" as "role1" is missing
            token = getTokenWithRole("role2");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);

            // roles allowed security check (created for @RolesAllowed) fails over missing role "role3"
            token = getTokenWithRole("role2", "role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("role3", "role2", "role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsAndClassicHttpSecurityPolicyWithRbac() {
        // there are 2 HTTP Permission checks for this path, one happens before tenant selection, one happens "after" it
        // also @Authenticated is applied
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(403);

            // permission check "combined-part1" as "role2" is missing
            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(403);

            // permission check "combined-part2" as "role1" is missing
            token = getTokenWithRole("role2");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("role2", "role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsIdentityAugmentation() {
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // pass JAX-RS permission check but missing permission
            String token = getTokenWithRole("role2");
            AuthEventObserver.clearEvents();
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-identity-augmentation")
                    .then().statusCode(403);
            Assertions.assertEquals(1, AuthEventObserver.getAuthorizationFailureEvents().size());

            token = getTokenWithRole("role3");
            AuthEventObserver.clearEvents();
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-identity-augmentation")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));
            // expect one JAX-RS HTTP Permission check and one Security check for the PermissionsAllowed annotation
            Assertions.assertEquals(2, AuthEventObserver.getAuthorizationSuccessEvents().size());
            AuthorizationSuccessEvent successEvent = AuthEventObserver
                    .getAuthorizationSuccessEvents()
                    .stream()
                    .filter(ev -> AbstractPathMatchingHttpSecurityPolicy.class.getName()
                            .equals(ev.getEventProperties().get(AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT)))
                    .findFirst()
                    .orElseThrow();

            token = getTokenWithRole("role4");
            AuthEventObserver.clearEvents();
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-identity-augmentation")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));
            // quarkus.http.auth.roles-mapping mapped role4 to role3, check that role3 is already present inside authN event
            Assertions.assertEquals(1, AuthEventObserver.getAuthenticationSuccessEvents().size());
            SecurityIdentity identity = AuthEventObserver.getAuthenticationSuccessEvents().get(0)
                    .getSecurityIdentity();
            Assertions.assertNotNull(identity);
            Assertions.assertTrue(identity.hasRole("role3"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void testPolicyAppliedBeforeTenantAnnotationMatched() {
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // policy applied before @Tenant annotation has been matched and different tenant has been used for auth
            // than the one that @Tenant annotation selects
            var token = getNonHrTenantAccessToken(Set.of("admin"));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/http-security-policy-applies-all-diff")
                    .then().statusCode(401);

            // policy applied before @Tenant annotation has been matched and different tenant has been used for auth
            // than the one that @Tenant annotation selects
            token = getTokenWithRole("admin");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/http-security-policy-applies-all-same")
                    .then().statusCode(200)
                    .body(Matchers
                            .equalTo("tenant-id=null, static.tenant.id=hr, name=alice, tenant-id-set-by-annotation=null"));
        } finally {
            server.stop();
        }
    }

    private static String getTokenWithRole(String... roles) {
        return Jwt.preferredUserName("alice")
                .groups(Set.of(roles))
                .audience("http://hr.service")
                .jws()
                .keyId("1")
                .sign("privateKey.jwk");
    }

    private String getNonHrTenantAccessToken(Set<String> groups) {
        return Jwt.preferredUserName("alice")
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws()
                .keyId("1")
                .sign();
    }
}
