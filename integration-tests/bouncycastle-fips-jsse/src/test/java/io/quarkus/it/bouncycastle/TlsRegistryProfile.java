package io.quarkus.it.bouncycastle;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Test profile that uses TLS registry
 */
public class TlsRegistryProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "tls-registry";
    }
}
