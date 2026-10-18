package io.quarkus.it.kafka.devservices.profiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesDisabledProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("quarkus.kafka.devservices.enabled", "false");
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
