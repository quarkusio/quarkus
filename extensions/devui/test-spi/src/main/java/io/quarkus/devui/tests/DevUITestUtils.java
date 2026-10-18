package io.quarkus.devui.tests;

import java.net.URI;

import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.smallrye.config.Config;

public class DevUITestUtils {
    static final String DOT = ".";
    static final RuntimeKey<URI> LOCAL_BASE_URI = RuntimeKey.key("quarkus.http.local-base-uri");

    private DevUITestUtils() {
        throw new UnsupportedOperationException();
    }

    public static String managementRootPath(Config config, String... paths) {
        String rootPath = config.getOptionalValue("quarkus.management.root-path", String.class).orElse("/q");
        StringBuilder path = new StringBuilder(rootPath);
        if (!rootPath.startsWith("/")) {
            path.insert(0, "/");
        }
        if (!rootPath.endsWith("/")) {
            path.append("/");
        }
        for (String p : paths) {
            String relativePath = p.startsWith("/") ? p.substring(1) : p;
            path.append(relativePath);
        }
        return path.toString();
    }
}
