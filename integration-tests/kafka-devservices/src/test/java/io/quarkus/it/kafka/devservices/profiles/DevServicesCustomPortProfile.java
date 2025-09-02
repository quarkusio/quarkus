package io.quarkus.it.kafka.devservices.profiles;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesCustomPortProfile implements QuarkusTestProfile {

    public static final String PORT = "5050";

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.kafka.devservices.provider", "kafka-native",
                "quarkus.kafka.devservices.port", PORT);
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
