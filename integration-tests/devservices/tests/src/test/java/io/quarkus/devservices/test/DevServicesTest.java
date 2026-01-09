package io.quarkus.devservices.test;

import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_BASE_URL;
import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_STATIC_THING;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DevServicesTest {
    @ConfigProperty(name = QUARKUS_SIMPLE_EXTENSION_BASE_URL)
    String url;

    @ConfigProperty(name = QUARKUS_SIMPLE_EXTENSION_STATIC_THING)
    String staticProperty;

    @Test
    public void testTheLazyDevServicesConfigIsAvailable() {
        assertNotNull(url);
    }

    @Test
    public void testTheBuildTimeServicesConfigIsAvailable() {
        assertNotNull(staticProperty);
    }

    @Test
    public void testServiceIsListeningOnTheDeclaredPort() {
        given()
                .relaxedHTTPSValidation()
                .when()
                .head(url)
                .then()
                .statusCode(200);
    }
}
