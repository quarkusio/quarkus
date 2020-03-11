package io.quarkus.undertow.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SecuredAnnotationServlet.class));

    @Test
    public void testServletSecurityAnnotation() {
        RestAssured.when().get("/annotation/servlet").then()
                .statusCode(401);
    }

}
