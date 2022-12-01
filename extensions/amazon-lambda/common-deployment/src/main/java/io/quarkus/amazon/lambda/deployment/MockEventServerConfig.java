package io.quarkus.amazon.lambda.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration for the mock event server that is run
 * in dev mode and test mode
 */
@ConfigGroup
public class MockEventServerConfig {
    /**
     * Port to access mock event server in dev mode
     */
    @ConfigItem(defaultValue = "8080")
    public int devPort;

    /**
     * Port to access mock event server in dev mode
     */
    @ConfigItem(defaultValue = "8081")
    public int testPort;
}
