package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.WithDefault;

/**
 * Configure the global traffic shaping functionality.
 * It allows you to limit the bandwidth across all channels, regardless of the number of open channels.
 * This can be useful when you want to control the overall network traffic to prevent congestion
 * or prioritize certain types of traffic.
 * <p>
 * The traffic shaping allows you to configure various parameters, such as write and read limitations (in bytes per
 * second), check interval (the delay between two computations of the bandwidth), and maximum time to wait.
 * The check interval represents the period at which the traffic is computed, and a higher interval may result in
 * less precise traffic shaping. It is recommended to set a positive value for the check interval, even if it is high,
 * to ensure traffic shaping without accounting. A suggested value is something close to 5 or 10 minutes.
 * <p>
 * The `outbound-global-bandwidth` and `inbound-global-bandwidth` parameters represent the maximum number of bytes per second
 * for write and read operations, respectively.
 * Additionally, you can set the maximum time to wait, which specifies an upper bound for time shaping.
 * By default, it is set to 15 seconds.
 */
public interface TrafficShapingConfig {
    /**
     * Enables the traffic shaping.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Set bandwidth limit in bytes per second for inbound connections, up to {@code Long.MAX_VALUE} bytes.
     * If not set, no limits are applied.
     */
    Optional<MemorySize> inboundGlobalBandwidth();

    /**
     * Set bandwidth limit in bytes per second for outbound connections, up to {@code Long.MAX_VALUE} bytes.
     * If not set, no limits are applied.
     */
    Optional<MemorySize> outboundGlobalBandwidth();

    /**
     * Set the maximum delay to wait in case of traffic excess.
     * Default is 15s. Must be less than the HTTP timeout.
     */
    Optional<Duration> maxDelay();

    /**
     * Set the delay between two computations of performances for channels.
     * If set to 0, no stats are computed.
     * Despite 0 is accepted (no accounting), it is recommended to set a positive value for the check interval,
     * even if it is high since the precision of the traffic shaping depends on the period where the traffic is computed.
     * In this case, a suggested value is something close to 5 or 10 minutes.
     * <p>
     * If not default, it defaults to 1s.
     */
    Optional<Duration> checkInterval();

    /**
     * Set the maximum global write size in bytes per second allowed in the buffer globally for all channels before write
     * are suspended, up to {@code Long.MAX_VALUE} bytes.
     * The default value is 400 MB.
     */
    Optional<MemorySize> peakOutboundGlobalBandwidth();
}
