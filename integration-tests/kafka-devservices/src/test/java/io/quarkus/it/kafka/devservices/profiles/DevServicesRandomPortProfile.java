package io.quarkus.it.kafka.devservices.profiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesRandomPortProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // Don't set a port, to exercise the random port path
        return Collections.emptyMap();
    }
}
