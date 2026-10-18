package io.quarkus.funqy.lambda.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.funqy.amazon-lambda")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface FunqyAmazonBuildTimeConfig {

    /**
     * The advanced event handling config
     */
    @WithName("advanced-event-handling")
    AdvancedEventHandlingBuildTimeConfig advancedEventHandling();
}
