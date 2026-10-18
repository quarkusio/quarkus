package org.acme;

import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.test.junit.QuarkusTestProfile;

public class SomeProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {

        // Access config
        ConfigProvider.getConfig().getPropertyNames();

        return QuarkusTestProfile.super.getConfigOverrides();
    }
}
