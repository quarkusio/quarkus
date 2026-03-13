package io.quarkus.it.bouncycastle;

import io.quarkus.test.junit.QuarkusTestProfile;

public class HttpSslProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "http-ssl";
    }
}
