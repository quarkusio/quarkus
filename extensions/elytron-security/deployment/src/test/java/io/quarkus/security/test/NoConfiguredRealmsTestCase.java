package io.quarkus.security.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Validate that a deployment with the security extension but no configured realms does not blow up
 */
public class NoConfiguredRealmsTestCase {
    static Class[] testClasses = {
            TestSecureServlet.class
    };
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsManifestResource(new StringAsset("quarkus.security.file.enabled=false"),
                            "microprofile-config.properties"));

    /**
     * Should fail with 403 rather than 401 as there is no authentication enabled, but the servlet requires roles
     */
    @Test()
    public void testSecureAccessFailure() {
        RestAssured.when().get("/secure-test").then()
                .statusCode(403);
    }
}
