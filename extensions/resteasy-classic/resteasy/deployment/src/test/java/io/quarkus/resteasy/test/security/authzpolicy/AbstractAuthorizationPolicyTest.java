package io.quarkus.resteasy.test.security.authzpolicy;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.restassured.RestAssured;

public abstract class AbstractAuthorizationPolicyTest {

    protected static final Class<?>[] TEST_CLASSES = { TestIdentityProvider.class, TestIdentityController.class,
            ForbidAllButViewerAuthorizationPolicy.class, ForbidViewerClassLevelPolicyResource.class,
            ForbidViewerMethodLevelPolicyResource.class, NoAuthorizationPolicyResource.class,
            PermitUserAuthorizationPolicy.class, ClassRolesAllowedMethodAuthZPolicyResource.class,
            ClassAuthZPolicyMethodRolesAllowedResource.class, ViewerAugmentingPolicy.class,
            AuthorizationPolicyAndPathMatchingPoliciesResource.class };

    protected static final String APPLICATION_PROPERTIES = """
            quarkus.http.auth.policy.admin-role.roles-allowed=admin
            quarkus.http.auth.policy.viewer-role.roles-allowed=viewer
            quarkus.http.auth.permission.jax-rs1.paths=/no-authorization-policy/jax-rs-path-matching-http-perm
            quarkus.http.auth.permission.jax-rs1.policy=admin-role
            quarkus.http.auth.permission.jax-rs1.applies-to=JAXRS
            quarkus.http.auth.permission.standard1.paths=/no-authorization-policy/path-matching-http-perm
            quarkus.http.auth.permission.standard1.policy=admin-role
            quarkus.http.auth.permission.jax-rs2.paths=/authz-policy-and-path-matching-policies/jax-rs-path-matching-http-perm
            quarkus.http.auth.permission.jax-rs2.policy=viewer-role
            quarkus.http.auth.permission.jax-rs2.applies-to=JAXRS
            quarkus.http.auth.permission.standard2.paths=/authz-policy-and-path-matching-policies/path-matching-http-perm
            quarkus.http.auth.permission.standard2.policy=viewer-role
            """;

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin", "viewer")
                .add("user", "user")
                .add("viewer", "viewer", "viewer");
    }

    @Test
    public void testNoAuthorizationPolicy() {
        // unsecured endpoint
        RestAssured.given().auth().preemptive().basic("viewer", "viewer").get("/no-authorization-policy/unsecured")
                .then().statusCode(200).body(Matchers.equalTo("viewer"));

        // secured with JAX-RS path-matching roles allowed HTTP permission requiring 'admin' role
        RestAssured.given().auth().preemptive().basic("user", "user")
                .get("/no-authorization-policy/jax-rs-path-matching-http-perm")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("admin", "admin")
                .get("/no-authorization-policy/jax-rs-path-matching-http-perm")
                .then().statusCode(200).body(Matchers.equalTo("admin"));

        // secured with path-matching roles allowed HTTP permission requiring 'admin' role
        RestAssured.given().auth().preemptive().basic("user", "user").get("/no-authorization-policy/path-matching-http-perm")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/no-authorization-policy/path-matching-http-perm")
                .then().statusCode(200).body(Matchers.equalTo("admin"));

        // secured with @RolesAllowed("admin")
        RestAssured.given().auth().preemptive().basic("user", "user").get("/no-authorization-policy/roles-allowed-annotation")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/no-authorization-policy/roles-allowed-annotation")
                .then().statusCode(200).body(Matchers.equalTo("admin"));
    }

    @Test
    public void testMethodLevelAuthorizationPolicy() {
        // policy placed on the endpoint directly, requires 'viewer' principal and must not pass anyone else
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/forbid-viewer-method-level-policy")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("viewer", "viewer").get("/forbid-viewer-method-level-policy")
                .then().statusCode(200).body(Matchers.equalTo("viewer"));

        // which means the other endpoint inside same resource class must not be affected by the policy
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/forbid-viewer-method-level-policy/unsecured")
                .then().statusCode(200).body(Matchers.equalTo("admin"));
    }

    @Test
    public void testClassLevelAuthorizationPolicy() {
        // policy placed on the resource, requires 'viewer' principal and must not pass anyone else
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/forbid-viewer-class-level-policy")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("viewer", "viewer").get("/forbid-viewer-class-level-policy")
                .then().statusCode(200).body(Matchers.equalTo("viewer"));
    }

    @Test
    public void testAuthorizationPolicyOnMethodAndRolesAllowedOnClass() {
        // class with @RolesAllowed("admin")
        // method with @AuthorizationPolicy(policy = "permit-user")
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/roles-allowed-class-authorization-policy-method")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("user", "user").get("/roles-allowed-class-authorization-policy-method")
                .then().statusCode(200).body(Matchers.equalTo("user"));

        // no @AuthorizationPolicy on method, therefore require admin
        RestAssured.given().auth().preemptive().basic("user", "user")
                .get("/roles-allowed-class-authorization-policy-method/no-authz-policy")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("admin", "admin")
                .get("/roles-allowed-class-authorization-policy-method/no-authz-policy")
                .then().statusCode(200).body(Matchers.equalTo("admin"));
    }

    @Test
    public void testAuthorizationPolicyOnClassRolesAllowedOnMethod() {
        // class with @AuthorizationPolicy(policy = "permit-user")
        // method with @RolesAllowed("admin")
        RestAssured.given().auth().preemptive().basic("user", "user").get("/authorization-policy-class-roles-allowed-method")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/authorization-policy-class-roles-allowed-method")
                .then().statusCode(200).body(Matchers.equalTo("admin"));

        // class with @AuthorizationPolicy(policy = "permit-user")
        // method has no annotation, therefore expect to permit only the user
        RestAssured.given().auth().preemptive().basic("admin", "admin")
                .get("/authorization-policy-class-roles-allowed-method/no-roles-allowed")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("user", "user")
                .get("/authorization-policy-class-roles-allowed-method/no-roles-allowed")
                .then().statusCode(200).body(Matchers.equalTo("user"));
    }

    @Test
    public void testCombinationOfAuthzPolicyAndPathConfigPolicies() {
        // ViewerAugmentingPolicy adds 'admin' role to the viewer

        // here we test that both @AuthorizationPolicy and path-matching policies work together
        // viewer role is required by (JAX-RS) path-matching HTTP policies,
        RestAssured.given().auth().preemptive().basic("admin", "admin")
                .get("/authz-policy-and-path-matching-policies/jax-rs-path-matching-http-perm")
                .then().statusCode(200).body(Matchers.equalTo("true"));
        RestAssured.given().auth().preemptive().basic("viewer", "viewer")
                .get("/authz-policy-and-path-matching-policies/jax-rs-path-matching-http-perm")
                .then().statusCode(200).body(Matchers.equalTo("true"));
        RestAssured.given().auth().preemptive().basic("user", "user")
                .get("/authz-policy-and-path-matching-policies/jax-rs-path-matching-http-perm")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("admin", "admin")
                .get("/authz-policy-and-path-matching-policies/path-matching-http-perm")
                .then().statusCode(200).body(Matchers.equalTo("true"));
        RestAssured.given().auth().preemptive().basic("viewer", "viewer")
                .get("/authz-policy-and-path-matching-policies/path-matching-http-perm")
                .then().statusCode(200).body(Matchers.equalTo("true"));
        RestAssured.given().auth().preemptive().basic("user", "user")
                .get("/authz-policy-and-path-matching-policies/path-matching-http-perm")
                .then().statusCode(403);

        // endpoint is annotated with @RolesAllowed("admin"), therefore class-level @AuthorizationPolicy is not applied
        RestAssured.given().auth().preemptive().basic("admin", "admin")
                .get("/authz-policy-and-path-matching-policies/roles-allowed-annotation")
                .then().statusCode(200).body(Matchers.equalTo("admin"));
        RestAssured.given().auth().preemptive().basic("viewer", "viewer")
                .get("/authz-policy-and-path-matching-policies/roles-allowed-annotation")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("user", "user")
                .get("/authz-policy-and-path-matching-policies/roles-allowed-annotation")
                .then().statusCode(403);
    }
}
