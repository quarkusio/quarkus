package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RolesAllowedServletTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RolesAllowedAnnotationServlet.class, RolesAllowedBeanServlet.class, UserServlet.class,
                            TestIdentityProvider.class,
                            TestIdentityController.class));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testRolesAllowed() {
        RestAssured.get("/roles-bean").then().statusCode(401);
        RestAssured.given().auth().basic("admin", "admin").get("/roles-bean").then().statusCode(200);
        RestAssured.given().auth().basic("admin", "wrong").get("/roles-bean").then().statusCode(401);
        RestAssured.given().auth().basic("user", "user").get("/roles-bean").then().statusCode(403);

        RestAssured.get("/roles-anno").then().statusCode(401);
        RestAssured.given().auth().basic("admin", "admin").get("/roles-anno").then().statusCode(200);
        RestAssured.given().auth().basic("admin", "wrong").get("/roles-anno").then().statusCode(401);
        RestAssured.given().auth().basic("user", "user").get("/roles-anno").then().statusCode(403);
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
