package io.quarkus.security.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.undertow.servlet.ServletExtension;

public class CustomAuthTestCase {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestSecureServlet.class, CustomAuth.class, CustomAuthExtension.class, CustomAuthFactory.class)
                    .addAsManifestResource("microprofile-config-custom-auth.properties", "microprofile-config.properties")
                    .addAsManifestResource(new StringAsset(CustomAuthExtension.class.getName()),
                            "services/" + ServletExtension.class.getName())
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
