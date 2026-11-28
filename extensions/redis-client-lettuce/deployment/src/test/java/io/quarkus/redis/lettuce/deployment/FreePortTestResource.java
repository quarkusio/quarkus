package io.quarkus.redis.lettuce.deployment;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Provides {@code lettuce.test.free-port}: a host port nothing listens on, for the tests of what the Lettuce backend
 * does when its server is away.
 */
public class FreePortTestResource implements QuarkusTestResourceLifecycleManager {

    static final String PORT = "lettuce.test.free-port";

    @Override
    public Map<String, String> start() {
        return Map.of(PORT, String.valueOf(TestPorts.free(1).get(0)));
    }

    @Override
    public void stop() {
    }

}
