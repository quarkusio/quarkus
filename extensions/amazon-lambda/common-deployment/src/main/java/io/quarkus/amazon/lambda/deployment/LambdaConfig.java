package io.quarkus.amazon.lambda.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.lambda")
public interface LambdaConfig {

    /**
     * Configuration for the mock event server that is run
     * in dev mode and test mode
     */
    MockEventServerConfig mockEventServer();
}
