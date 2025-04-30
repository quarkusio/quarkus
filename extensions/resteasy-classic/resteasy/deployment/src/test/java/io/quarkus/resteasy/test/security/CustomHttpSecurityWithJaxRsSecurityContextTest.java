package io.quarkus.resteasy.test.security;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomHttpSecurityWithJaxRsSecurityContextTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(CustomPolicyResource.class, TestIdentityProvider.class,
                            TestIdentityController.class, CustomHttpSecurityPolicy.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.permission.custom-policy-1.paths=/custom-policy/is-admin
                            quarkus.http.auth.permission.custom-policy-1.policy=custom
                            quarkus.http.auth.permission.custom-policy-1.applies-to=JAXRS
                            """),
                            "application.properties"));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("test", "test", "test")
                .add("user", "user", "user");
    }

    @Test
    public void testAugmentedIdentityInSecurityContext() {
        // test that custom HTTP Security Policy is applied, it added 'admin' role to the 'user'
        // and this new role is present in the JAX-RS SecurityContext
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .get("/custom-policy/is-admin")
                .then()
                .statusCode(200)
                .body(Matchers.is("true"));
        RestAssured
                .given()
                .auth().preemptive().basic("test", "test")
                .get("/custom-policy/is-admin")
                .then()
                .statusCode(200)
                .body(Matchers.is("false"));
    }

}
