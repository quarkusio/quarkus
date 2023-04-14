package io.quarkus.observability.test.support;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class VmTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.observability.vm-agent.enabled", "false");
    }
}
