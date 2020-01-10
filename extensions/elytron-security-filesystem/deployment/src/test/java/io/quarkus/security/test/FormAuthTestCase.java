package io.quarkus.security.test;

import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.given;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.authentication.FormAuthConfig;

/**
 * Tests of FORM authentication mechanism
 * <p>
 * See @io.quarkus.vertx.http.security.FormAuthTestCase for functional coverage.
 */
public class FormAuthTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestSecureServlet.class, TestApplication.class, RolesEndpointClassLevel.class)
                    .addAsResource("application-form-auth.properties", "application.properties"));

    @Test()
    public void testSecureAccessSuccess() {
        enableLoggingOfRequestAndResponseIfValidationFails();
        given().auth()
                .form("stuart", "test",
                        new FormAuthConfig("j_security_check", "j_username", "j_password")
                                .withLoggingEnabled())
                .when().get("/secure-test").then().statusCode(200);

        given().auth()
                .form("jdoe", "p4ssw0rd",
                        new FormAuthConfig("j_security_check", "j_username", "j_password")
                                .withLoggingEnabled())
                .when().get("/secure-test").then().statusCode(403);

        given().auth()
                .form("scott", "jb0ss",
                        new FormAuthConfig("j_security_check", "j_username", "j_password")
                                .withLoggingEnabled())
                .when().get("/jaxrs-secured/rolesClass").then().statusCode(200);
    }

}
