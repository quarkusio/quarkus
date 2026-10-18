package io.quarkus.it.kafka.devservices.profiles;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesNonUniquePortProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // Set an image, both for coverage, and so we know we're connecting to the right service
        Map<String, String> overrides = new HashMap<>();
        overrides.put("quarkus.kafka.devservices.port", "5050");
        // Choose an image which is different what other tests use, so we can identify we're running on it
        overrides.put("quarkus.kafka.devservices.provider", "Redpanda");
        return overrides;
    }
}
