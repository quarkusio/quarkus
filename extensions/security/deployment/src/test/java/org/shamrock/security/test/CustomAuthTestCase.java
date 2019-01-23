package org.shamrock.security.test;

import io.restassured.RestAssured;
import io.undertow.servlet.ServletExtension;
import org.jboss.shamrock.test.Deployment;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockUnitTest.class)
public class CustomAuthTestCase {
    @Deployment
    public static JavaArchive deploy() {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        StringAsset customAuthExt = new StringAsset(CustomAuthExtension.class.getName());
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class)
                .addClasses(TestSecureServlet.class, CustomAuth.class, CustomAuthExtension.class, CustomAuthFactory.class)
                .addAsManifestResource("microprofile-config-custom-auth.properties", "microprofile-config.properties")
                .addAsManifestResource(customAuthExt, "services/"+ ServletExtension.class.getName())
                //.addAsManifestResource("logging.properties")
                .addAsResource("test-users.properties")
                .addAsResource("test-roles.properties")
                ;
        System.out.printf("BasicAuthApp: %s\n", archive.toString(true));
        return archive;
    }

    // Basic @ServletSecurity test
    @Test()
    public void testSecureAccessFailure() {
        RestAssured.when().get("/secure-test").then()
                .statusCode(401);
    }

    @Test()
    public void testSecureAccessSuccess() {
        System.out.printf("Begin testSecureAccessSuccess\n");
        RestAssured.given().auth().preemptive().basic("stuart", "test")
                .when().get("/secure-test").then()
                .statusCode(200);
    }
}
