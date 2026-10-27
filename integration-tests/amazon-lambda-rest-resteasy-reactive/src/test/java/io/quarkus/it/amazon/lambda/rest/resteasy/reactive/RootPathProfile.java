package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RootPathProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.http.root-path", "/svc");
    }
}
