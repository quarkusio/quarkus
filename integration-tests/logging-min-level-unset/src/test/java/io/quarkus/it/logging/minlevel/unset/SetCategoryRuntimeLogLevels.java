package io.quarkus.it.logging.minlevel.unset;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public final class SetCategoryRuntimeLogLevels implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        final Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("quarkus.log.category.\"io.quarkus.it.logging.minlevel.unset.above\".level", "WARN");
        systemProperties.put("quarkus.log.category.\"io.quarkus.it.logging.minlevel.unset.promote\".level", "TRACE");
        return systemProperties;
    }

    @Override
    public void stop() {
    }

}
