package io.quarkus.it.keycloak;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TlsRegistryTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "tls-registry";
    }
}
