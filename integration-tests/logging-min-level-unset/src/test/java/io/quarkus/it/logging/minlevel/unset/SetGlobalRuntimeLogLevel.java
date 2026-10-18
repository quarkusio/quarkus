package io.quarkus.it.logging.minlevel.unset;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public final class SetGlobalRuntimeLogLevel implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        final Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("quarkus.log.level", "TRACE");
        return systemProperties;
    }

    @Override
    public void stop() {
    }

}
