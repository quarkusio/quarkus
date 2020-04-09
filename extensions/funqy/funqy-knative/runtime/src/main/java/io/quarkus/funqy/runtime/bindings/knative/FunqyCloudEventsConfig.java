package io.quarkus.funqy.runtime.bindings.knative;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class FunqyCloudEventsConfig {

    /**
     * Cloud-event source that will be returned in response.
     */
    @ConfigItem
    Optional<String> source;

    /**
     * Cloud-event type that will be returned in response.
     */
    @ConfigItem
    Optional<String> type;
}
