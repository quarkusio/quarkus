package io.quarkus.it.amazon.lambda.profiles;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TrivialProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.lambda.handler", "test");
    }

}
