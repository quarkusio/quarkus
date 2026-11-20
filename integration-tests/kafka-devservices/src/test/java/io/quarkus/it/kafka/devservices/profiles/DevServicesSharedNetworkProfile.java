package io.quarkus.it.kafka.devservices.profiles;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesSharedNetworkProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.devservices.launch-on-shared-network", "true");
    }
}
