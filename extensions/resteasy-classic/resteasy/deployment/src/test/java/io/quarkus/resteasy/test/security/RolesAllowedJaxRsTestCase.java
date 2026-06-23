package io.quarkus.resteasy.test.security;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RolesAllowedJaxRsTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RolesAllowedResource.class, UserResource.class,
                            TestIdentityProvider.class,
                            TestIdentityController.class,
                            UnsecuredSubResource.class));

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
        RestAssured.given().auth().basic("user", "user").get("/roles").then().statusCode(200);
        RestAssured.given().auth().basic("admin", "admin").get("/roles/admin").then().statusCode(200);
        RestAssured.given().auth().basic("user", "user").get("/roles/admin").then().statusCode(403);
    }

    @Test
    public void testRolesAllowedPathNormalization() {
        // GHSA-qcxp-gm7m-4j5v: verify no encoded path vector bypasses @RolesAllowed.
        // REST routing doesn't decode %3B/%2F/%5C, so the route won't match and
        // requests get 404. The security invariant: an admin-only endpoint must never
        // return 200 to an unauthenticated or unauthorized user via any encoding trick.
        for (String path : new String[] {
                "/roles/admin%3B",
                "/roles/admin%3Bbypass",
                "/roles%3B/admin",
                "/roles%2Fadmin",
                "/roles%5Cadmin",
                "/roles%252Fadmin" }) {
            int anonStatus = RestAssured.get(path).then().extract().statusCode();
            assertNotEquals(200, anonStatus, "anonymous access must not return 200 for " + path);
            int userStatus = RestAssured.given().auth().basic("user", "user")
                    .get(path).then().extract().statusCode();
            assertNotEquals(200, userStatus, "user (non-admin) must not return 200 for " + path);
        }
    }

    @Test
    public void testUser() {
        RestAssured.get("/user").then().body(is(""));
        RestAssured.given().auth().basic("admin", "admin").get("/user").then().body(is(""));
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/user").then().body(is("admin"));
        RestAssured.given().auth().basic("user", "user").get("/user").then().body(is(""));
        RestAssured.given().auth().preemptive().basic("user", "user").get("/user").then().body(is("user"));
    }
}
