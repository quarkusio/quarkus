package io.quarkus.it.logging.minlevel.set;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public final class SetRuntimeLogLevels implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        final Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("quarkus.log.category.\"io.quarkus.it.logging.minlevel.set.bydefault\".level", "DEBUG");
        systemProperties.put("quarkus.log.category.\"io.quarkus.it.logging.minlevel.set.above\".level", "WARN");
        systemProperties.put("quarkus.log.category.\"io.quarkus.it.logging.minlevel.set.below\".level", "TRACE");
        systemProperties.put("quarkus.log.category.\"io.quarkus.it.logging.minlevel.set.below.child\".level", "inherit");
        systemProperties.put("quarkus.log.category.\"io.quarkus.it.logging.minlevel.set.promote\".level", "INFO");
        return systemProperties;
    }

    @Override
    public void stop() {
    }

}
