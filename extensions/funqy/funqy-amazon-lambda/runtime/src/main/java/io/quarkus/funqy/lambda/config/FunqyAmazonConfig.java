package io.quarkus.funqy.lambda.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.funqy.amazon-lambda")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface FunqyAmazonConfig {

    /**
     * The advanced event handling config
     */
    @WithName("advanced-event-handling")
    AdvancedEventHandlingConfig advancedEventHandling();
}
