package io.quarkus.security.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomAuthTestCase {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestSecureServlet.class, CustomAuth.class)
                    .addAsResource("application-custom-auth.properties", "application.properties")
                    //.addAsManifestResource("logging.properties")
                    .addAsResource("test-users.properties")
                    .addAsResource("test-roles.properties"));

    // Basic @ServletSecurity test
    @Test()
    public void testSecureAccessFailure() {
        RestAssured.when().get("/secure-test").then()
                .statusCode(401);
    }

    @Test()
    public void testSecureAccessSuccess() {
        RestAssured.given().auth().preemptive().basic("stuart", "test")
                .when().get("/secure-test").then()
                .statusCode(200);
    }
}
