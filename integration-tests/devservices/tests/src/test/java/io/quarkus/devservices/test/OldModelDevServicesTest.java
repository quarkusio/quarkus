package io.quarkus.devservices.test;

import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_BASE_URL;
import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_STATIC_THING;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OldModelDevServicesTest {

    private static final String UNSET = "unset";

    @ConfigProperty(name = OLD_MODEL_EXTENSION_BASE_URL, defaultValue = UNSET)
    String oldModelExtensionBaseUrl;

    @ConfigProperty(name = OLD_MODEL_EXTENSION_STATIC_THING, defaultValue = UNSET)
    String oldModelStaticProperty;

    @Test
    public void testOldModelDevServicesConfigIsAvailable() {
        assertIsSet(oldModelExtensionBaseUrl);
    }

    @Test
    public void testOldModelDevServicesStaticConfigIsAvailable() {
        assertIsSet(oldModelStaticProperty);
    }

    @Test
    public void testOldModelServiceIsListeningOnTheDeclaredPort() {
        given()
                .relaxedHTTPSValidation()
                .when()
                .head(oldModelExtensionBaseUrl)
                .then()
                .statusCode(200);
    }

    private void assertIsSet(String value) {
        Assertions.assertNotNull(value);
        assertNotEquals(UNSET, value);
    }
}
