package io.quarkus.security.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests of {@link io.quarkus.elytron.security.runtime.DefaultRoleDecoder}
 */
public class CustomRoleDecoderTestCase {
    static Class[] testClasses = {
            TestApplication.class, CustomRoleDecoder.class, CustomRoleAccessResource.class
    };
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("application.properties"));

    @Test()
    public void testCustomRoleMapping() {
        RestAssured.given().auth().preemptive().basic("alice", "alice")
                .when().get("/jaxrs-secured/custom-role").then()
                .statusCode(200);
    }
}
