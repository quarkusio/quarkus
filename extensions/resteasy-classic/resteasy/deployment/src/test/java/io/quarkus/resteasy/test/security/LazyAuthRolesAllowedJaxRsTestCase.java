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

public class LazyAuthRolesAllowedJaxRsTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RolesAllowedResource.class, UserResource.class,
                            TestIdentityProvider.class,
                            TestIdentityController.class,
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
    public void testRolesAllowed() {
        RestAssured.get("/roles").then().statusCode(401);
        RestAssured.given().auth().basic("admin", "admin").get("/roles").then().statusCode(200);
        RestAssured.given().auth().basic("admin", "wrong").get("/roles").then().statusCode(401);
        RestAssured.given().auth().basic("user", "user").get("/roles").then().statusCode(200);
        RestAssured.given().auth().basic("admin", "admin").get("/roles/admin").then().statusCode(200);
        RestAssured.given().auth().basic("user", "user").get("/roles/admin").then().statusCode(403);
    }

    @Test
    public void testUser() {
        RestAssured.get("/user").then().body(is(""));
        RestAssured.given().auth().basic("admin", "admin").get("/user").then().body(is(""));
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/user").then().body(is(""));
        RestAssured.given().auth().basic("user", "user").get("/user").then().body(is(""));
        RestAssured.given().auth().preemptive().basic("user", "user").get("/user").then().body(is(""));
    }
}
