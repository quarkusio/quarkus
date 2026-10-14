package org.acme;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.smallrye.config.Config;

public class SomeProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {

        // Access config. The profile runs before the application, so the configuration may not exist yet
        Config.getOrCreate().getPropertyNames();

        return QuarkusTestProfile.super.getConfigOverrides();
    }
}
