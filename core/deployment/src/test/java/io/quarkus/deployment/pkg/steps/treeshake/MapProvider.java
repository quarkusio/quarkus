package io.quarkus.deployment.pkg.steps.treeshake;

import java.util.HashMap;

/**
 * Extends HashMap, stores a class name as a map value, and calls {@code Class.forName()}
 * to trigger class-loading chain detection. Tests map value extraction (BouncyCastle pattern).
 */
public class MapProvider extends HashMap<String, Object> {
    public MapProvider() throws Exception {
        put("key", "io.quarkus.deployment.pkg.steps.treeshake.MapTarget");
        Class.forName("io.quarkus.deployment.pkg.steps.treeshake.LoadedByMap");
    }
}
