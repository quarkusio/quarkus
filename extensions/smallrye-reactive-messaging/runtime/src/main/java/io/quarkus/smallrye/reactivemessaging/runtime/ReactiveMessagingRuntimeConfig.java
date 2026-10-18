package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.messaging")
public interface ReactiveMessagingRuntimeConfig {

    /**
     * Whether to enable the context propagation for connector channels
     */
    @WithName("connector-context-propagation")
    Optional<List<String>> connectorContextPropagation();

    /**
     * Used internally only. Users use <code>mp.messaging</code>.
     */
    @ConfigDocIgnore
    Map<String, Incoming> incoming();

    /**
     * Used internally only. Users use <code>mp.messaging</code>.
     */
    @ConfigDocIgnore
    Map<String, Outgoing> outgoing();

    interface ChannelDirection {
        /**
         * Used internally only. Users use <code>mp.messaging</code>.
         */
        @ConfigDocIgnore
        Optional<String> connector();

        /**
         * Used internally only. Users use <code>mp.messaging</code>.
         */
        @ConfigDocIgnore
        @WithDefault("true")
        boolean enabled();

        /**
         * Used internally only.
         */
        @ConfigDocIgnore
        @WithParentName
        Map<String, Optional<String>> channelConfig();
    }

    interface Incoming extends ChannelDirection {

    }

    interface Outgoing extends ChannelDirection {

    }
}
