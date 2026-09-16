package io.quarkus.it.observation.micrometer.opentelemetry;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(CustomizationProfile.class)
// FIXME
@Disabled("Profile seems not to work on native")
public class ObservationCustomizationIT extends ObservationCustomizationTest {
}
