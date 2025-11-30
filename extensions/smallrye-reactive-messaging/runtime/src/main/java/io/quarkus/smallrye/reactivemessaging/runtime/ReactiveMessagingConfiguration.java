package io.quarkus.smallrye.reactivemessaging.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.messaging")
public interface ReactiveMessagingConfiguration {

    /**
     * Whether Reactive Messaging metrics are published in case a metrics extension is present
     * (default to false).
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

    /**
     * Enables or disables the strict validation mode.
     */
    @WithDefault("false")
    boolean strict();

    /**
     * Execution mode for the Messaging signatures considered "blocking", defaults to "worker".
     * For the previous behaviour set to "event-loop".
     */
    @WithName("blocking.signatures.execution.mode")
    @WithDefault("worker")
    ExecutionMode blockingSignaturesExecutionMode();

    enum ExecutionMode {
        EVENT_LOOP,
        WORKER,
        VIRTUAL_THREAD
    }

    /**
     * Normalize the name of a given channel.
     *
     * Concatenate the channel name with double quotes when it contains dots.
     * <p>
     * Otherwise, the SmallRye Reactive Messaging only considers the
     * text up to the first occurrence of a dot as the channel name.
     *
     * @param name the channel name.
     * @return normalized channel name.
     */
    static String normalizeChannelName(String name) {
        return name != null && !name.startsWith("\"") && name.contains(".") ? "\"" + name + "\"" : name;
    }

    String CHANNEL_INCOMING_PROPERTY = "mp.messaging.incoming.%s.%s";
    String CHANNEL_OUTGOING_PROPERTY = "mp.messaging.outgoing.%s.%s";

    static String getChannelIncomingPropertyName(final String channelName, final String attribute) {
        return String.format(CHANNEL_INCOMING_PROPERTY, normalizeChannelName(channelName), attribute);
    }

    static String getChannelOutgoingPropertyName(final String channelName, final String attribute) {
        return String.format(CHANNEL_OUTGOING_PROPERTY, normalizeChannelName(channelName), attribute);
    }

    static String getChannelPropertyName(final String channelName, final String attribute, final boolean incoming) {
        return incoming ? getChannelIncomingPropertyName(channelName, attribute)
                : getChannelOutgoingPropertyName(channelName, attribute);
    }
}
