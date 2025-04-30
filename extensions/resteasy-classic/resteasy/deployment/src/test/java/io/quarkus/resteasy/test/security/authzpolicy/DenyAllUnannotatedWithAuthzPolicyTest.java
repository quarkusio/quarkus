package io.quarkus.resteasy.test.security.authzpolicy;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DenyAllUnannotatedWithAuthzPolicyTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForbidViewerClassLevelPolicyResource.class, ForbidViewerMethodLevelPolicyResource.class,
                            ForbidAllButViewerAuthorizationPolicy.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource(new StringAsset("quarkus.security.jaxrs.deny-unannotated-endpoints=true\n"),
                            "application.properties"));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin", "viewer")
                .add("user", "user")
                .add("viewer", "viewer", "viewer");
    }

    @Test
    public void testEndpointWithoutAuthorizationPolicyIsDenied() {
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/forbid-viewer-method-level-policy/unsecured")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("viewer", "viewer").get("/forbid-viewer-method-level-policy/unsecured")
                .then().statusCode(403);
    }

    @Test
    public void testEndpointWithAuthorizationPolicyIsNotDenied() {
        // test not denied for authorized
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/forbid-viewer-method-level-policy")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("viewer", "viewer").get("/forbid-viewer-method-level-policy")
                .then().statusCode(200).body(Matchers.equalTo("viewer"));
    }

}
