package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class CompressionProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.http.enable-compression", "true");
    }
}
