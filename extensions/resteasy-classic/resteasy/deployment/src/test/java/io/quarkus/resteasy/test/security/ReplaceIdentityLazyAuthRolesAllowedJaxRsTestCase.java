package io.quarkus.resteasy.test.security;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ReplaceIdentityLazyAuthRolesAllowedJaxRsTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RolesAllowedResource.class, UserResource.class,
                            TestIdentityProvider.class,
                            TestIdentityController.class,
                            SecurityOverrideFilter.class,
                            UnsecuredSubResource.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n"),
                            "application.properties"));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testRolesAllowedModified() {
        //make sure that things work as normal when no modification happens
        RestAssured.given()
                .header("user", "admin")
                .header("role", "admin")
                .get("/roles").then().statusCode(200);
        RestAssured.given()
                .auth().basic("user", "user")
                .header("user", "admin")
                .header("role", "admin").get("/roles/admin").then().statusCode(200);
    }

    @Test
    public void testUser() {
        RestAssured.given().auth().basic("user", "user")
                .header("user", "admin")
                .header("role", "admin").get("/user").then().body(is("admin"));
        RestAssured.given().auth().preemptive().basic("user", "user")
                .header("user", "admin")
                .header("role", "admin").get("/user").then().body(is("admin"));
    }
}
