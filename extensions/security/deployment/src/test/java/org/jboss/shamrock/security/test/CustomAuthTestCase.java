package org.jboss.shamrock.security.test;

import io.restassured.RestAssured;
import io.undertow.servlet.ServletExtension;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


public class CustomAuthTestCase {
    @RegisterExtension
    static final ShamrockUnitTest config = new ShamrockUnitTest()
            .setArchiveProducer(() ->
                                        ShrinkWrap.create(JavaArchive.class)
                                                .addClasses(TestSecureServlet.class, CustomAuth.class, CustomAuthExtension.class, CustomAuthFactory.class)
                                                .addAsManifestResource("microprofile-config-custom-auth.properties", "microprofile-config.properties")
                                                .addAsManifestResource(new StringAsset(CustomAuthExtension.class.getName()), "services/"+ ServletExtension.class.getName())
                                                //.addAsManifestResource("logging.properties")
                                                .addAsResource("test-users.properties")
                                                .addAsResource("test-roles.properties")
            );

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
