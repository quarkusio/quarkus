package io.quarkus.it.micrometer.prometheus;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AdditionalHttpMethodsProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.micrometer.binder.http.additional-methods", "PROPFIND");
    }
}
