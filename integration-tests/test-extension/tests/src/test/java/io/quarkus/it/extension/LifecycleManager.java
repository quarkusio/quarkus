package io.quarkus.it.extension;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class LifecycleManager implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Counter.startCounter.incrementAndGet();
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        Counter.endCounter.incrementAndGet();
    }

}
