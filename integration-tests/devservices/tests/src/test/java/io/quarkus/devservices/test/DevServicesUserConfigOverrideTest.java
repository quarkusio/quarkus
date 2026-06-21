package io.quarkus.devservices.test;

import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_BASE_URL;
import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_STATIC_THING;
import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_BASE_URL;
import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_STATIC_THING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServicesUserConfigOverrideTest.Profile.class)
public class DevServicesUserConfigOverrideTest {

    private static final String UNSET = "unset";
    private static final String USER_PROVIDED_URL = "http://user-provided:9999";
    private static final String USER_PROVIDED_STATIC = "user-provided-static";

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    OLD_MODEL_EXTENSION_BASE_URL, USER_PROVIDED_URL,
                    OLD_MODEL_EXTENSION_STATIC_THING, USER_PROVIDED_STATIC);
        }
    }

    @ConfigProperty(name = OLD_MODEL_EXTENSION_BASE_URL, defaultValue = UNSET)
    String oldModelExtensionBaseUrl;

    @ConfigProperty(name = OLD_MODEL_EXTENSION_STATIC_THING, defaultValue = UNSET)
    String oldModelStaticProperty;

    @ConfigProperty(name = QUARKUS_SIMPLE_EXTENSION_BASE_URL, defaultValue = UNSET)
    String simpleExtensionBaseUrl;

    @ConfigProperty(name = QUARKUS_SIMPLE_EXTENSION_STATIC_THING, defaultValue = UNSET)
    String simpleStaticProperty;

    @Test
    public void testUserConfigOverridesOldModelDevServicesBaseUrl() {
        assertEquals(USER_PROVIDED_URL, oldModelExtensionBaseUrl);
    }

    @Test
    public void testUserConfigOverridesOldModelDevServicesStaticConfig() {
        assertEquals(USER_PROVIDED_STATIC, oldModelStaticProperty);
    }

    @Test
    public void testNewModelServiceStillStartsAlongsideUserConfig() {
        assertNotEquals(UNSET, simpleExtensionBaseUrl);
    }

    @Test
    public void testNewModelStaticConfigStillAvailableAlongsideUserConfig() {
        assertNotEquals(UNSET, simpleStaticProperty);
    }
}
