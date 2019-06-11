package io.quarkus.amazon.lambda.resteasy.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AmazonLambdaResteasyConfig {

    /**
     * Indicates if we are in debug mode.
     */
    @ConfigItem(defaultValue = "false")
    boolean debug;
}