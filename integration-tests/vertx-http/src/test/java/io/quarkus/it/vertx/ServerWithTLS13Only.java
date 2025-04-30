package io.quarkus.it.vertx;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ServerWithTLS13Only implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.tls.protocols", "TLSv1.3");
    }
}
