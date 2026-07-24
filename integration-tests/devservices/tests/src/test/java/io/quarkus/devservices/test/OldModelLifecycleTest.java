package io.quarkus.devservices.test;

import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_BASE_URL;
import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_START_COUNT_SYSTEM_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Verifies that old-model dev services get a fresh lifecycle for each test profile.
 *
 * Old-model processors use {@code static volatile} fields to cache running services.
 * When the augmentation classloader is properly scoped per profile, each profile loads
 * a fresh copy of the processor class with {@code devService == null}, triggering a
 * fresh container start. If the classloader is incorrectly shared across profiles,
 * the static field persists and the second profile reuses the first profile's container
 * instead of getting its own.
 *
 * This test reads the JVM-wide start-count system property (which is incremented each
 * time the processor freshly creates a container) at test execution time — after all
 * augmentations have completed. A count greater than 1 proves that multiple profiles
 * each received their own fresh processor instance.
 */
@QuarkusTest
@TestProfile(OldModelLifecycleTest.Profile.class)
public class OldModelLifecycleTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of();
        }
    }

    @ConfigProperty(name = OLD_MODEL_EXTENSION_BASE_URL, defaultValue = "unset")
    String baseUrl;

    @Test
    public void oldModelServiceWasFreshlyStartedForThisProfile() {
        int count = Integer.parseInt(System.getProperty(OLD_MODEL_START_COUNT_SYSTEM_PROPERTY, "0"));
        assertTrue(count > 1,
                "Old-model dev service should have been freshly started for each profile "
                        + "(start-count should be > 1 indicating multiple profiles each started their own instance), "
                        + "but start-count was " + count
                        + ". This suggests the service was reused across profiles due to shared static state.");
    }

    @Test
    public void oldModelServiceIsRunning() {
        assertNotEquals("unset", baseUrl);
    }
}
