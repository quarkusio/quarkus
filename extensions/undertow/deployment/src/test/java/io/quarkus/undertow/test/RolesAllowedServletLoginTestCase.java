package io.quarkus.undertow.test;

import static io.quarkus.undertow.test.LoginLogoutServlet.PASSWORD;
import static io.quarkus.undertow.test.LoginLogoutServlet.USER;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class RolesAllowedServletLoginTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(LoginLogoutServlet.class, RolesAllowedBeanServlet.class,
                    TestIdentityProvider.class, TestIdentityController.class));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles().add("admin", "admin", "admin").add("user", "user", "user");
    }

    @Test
    public void testRolesAllowed() {
        RestAssured.get("/roles-bean").then().statusCode(401);
        RestAssured.given().queryParams(Map.of(USER, "admin", PASSWORD, "wrong")).get("/login").then().statusCode(401);

        {
            final Response response = RestAssured.given().queryParams(Map.of(USER, "admin", PASSWORD, "admin"))
                    .get("/login");
            response.then().statusCode(200);
            final String sessionId = response.sessionId();

            RestAssured.given().sessionId(sessionId).get("/roles-bean").then().statusCode(200);
            RestAssured.given().sessionId(sessionId).get("/logout").then().statusCode(200);
            RestAssured.given().sessionId(sessionId).get("/roles-bean").then().statusCode(401);
        }

        {
            final Response response = RestAssured.given().queryParams(Map.of(USER, "user", PASSWORD, "user"))
                    .get("/login");
            response.then().statusCode(200);
            final String sessionId = response.sessionId();

            RestAssured.given().sessionId(sessionId).get("/roles-bean").then().statusCode(403);
            RestAssured.given().sessionId(sessionId).get("/logout").then().statusCode(200);
            RestAssured.given().sessionId(sessionId).get("/roles-bean").then().statusCode(401);
        }
    }
}
