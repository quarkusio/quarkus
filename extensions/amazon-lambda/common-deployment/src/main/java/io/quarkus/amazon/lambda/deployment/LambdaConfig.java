package io.quarkus.amazon.lambda.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class LambdaConfig {

    /**
     * Configuration for the mock event server that is run
     * in dev mode and test mode
     */
    MockEventServerConfig mockEventServer;
}
