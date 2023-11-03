package io.quarkus.undertow.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * tests that basic annotation security is applied. We don't actually have
 * the security subsystem installed here, so this is the fallback behaviour that
 * will always deny
 */
public class ServletSecurityTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredAnnotationServlet.class, TestIdentityController.class, TestIdentityProvider.class));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("admin", "admin", "admin");
    }

    @Test
    public void testServletSecurityAnnotation() {
        RestAssured.when().get("/annotation/servlet").then()
                .statusCode(401);
        RestAssured.given().auth().basic("admin", "admin").when().get("/annotation/servlet").then()
                .statusCode(200);
    }

}
