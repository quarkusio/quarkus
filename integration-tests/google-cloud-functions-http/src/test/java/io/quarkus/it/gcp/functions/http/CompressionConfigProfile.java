package io.quarkus.it.gcp.functions.http;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class CompressionConfigProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.http.virtual", "true",
                "quarkus.http.host-enabled", "false",
                "quarkus.http.enable-compression", "true",
                "quarkus.http.compressors", "deflate",
                "quarkus.http.compression-content-size-threshold", "1024");
    }
}
