package io.quarkus.vertx.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class SecureSocketWithKeyStoreTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = QuarkusExtensionTest.withSecuredConnection()
            .withApplicationRoot((jar) -> jar
                    .addClass(TestRoute.class)
                    .addAsResource("application-keystore.properties", "application.properties")
                    .addAsResource("keystore.jks"));

    @Test
    public void testSecureConnectionAvailable() {
        given().baseUri("https://localhost:" + System.getProperty("quarkus.http.testSslPort", "8444"))
                .relaxedHTTPSValidation()
                .when().get("/test").then()
                .statusCode(200)
                .body(is("test route"));
    }
}
