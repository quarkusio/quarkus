package io.quarkus.amazon.lambda.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the mock event server that is run
 * in dev mode and test mode
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.lambda")
public interface MockEventServerConfig {
    static final String DEV_PORT = "8080";
    static final String TEST_PORT = "8081";

    /**
     * Port to access mock event server in dev mode
     */
    @WithDefault(DEV_PORT)
    int devPort();

    /**
     * Port to access mock event server in dev mode
     */
    @WithDefault(TEST_PORT)
    int testPort();
}
