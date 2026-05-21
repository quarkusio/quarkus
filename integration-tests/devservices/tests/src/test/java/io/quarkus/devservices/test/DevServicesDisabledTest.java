package io.quarkus.devservices.test;

import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_BASE_URL;
import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_STATIC_THING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServicesDisabledTest.Profile.class)
public class DevServicesDisabledTest {

    private static final String UNSET = "unset";

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.devservices.enabled", "false");
        }
    }

    @ConfigProperty(name = OLD_MODEL_EXTENSION_BASE_URL, defaultValue = UNSET)
    String oldModelExtensionBaseUrl;

    @ConfigProperty(name = OLD_MODEL_EXTENSION_STATIC_THING, defaultValue = UNSET)
    String oldModelStaticProperty;

    @Test
    public void testOldModelServiceIsNotStartedWhenDevServicesDisabled() {
        assertEquals(UNSET, oldModelExtensionBaseUrl);
    }

    @Test
    public void testOldModelStaticConfigIsNotSetWhenDevServicesDisabled() {
        assertEquals(UNSET, oldModelStaticProperty);
    }
}
