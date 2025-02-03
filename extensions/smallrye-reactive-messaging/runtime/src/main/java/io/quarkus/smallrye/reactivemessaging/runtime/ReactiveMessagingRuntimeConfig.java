package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.messaging")
public interface ReactiveMessagingRuntimeConfig {

    /**
     * Whether to enable the context propagation for connector channels
     */
    @WithName("connector-context-propagation")
    Optional<List<String>> connectorContextPropagation();
}
