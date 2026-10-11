package io.quarkus.it.profile;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class LimitProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("app.limit", "3");
    }
}
