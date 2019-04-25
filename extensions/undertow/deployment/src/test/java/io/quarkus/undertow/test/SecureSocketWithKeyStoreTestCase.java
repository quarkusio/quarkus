package io.quarkus.undertow.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SecureSocketWithKeyStoreTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = QuarkusUnitTest.withSecuredConnection()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestServlet.class)
                    .addAsResource("application-keystore.properties", "application.properties")
                    .addAsResource("keystore.jks"));

    @Test
    public void testSecureConnectionAvailable() {
        given()
                .relaxedHTTPSValidation()
                .when().get("/test").then()
                .statusCode(200)
                .body(is("test servlet"));
    }
}
