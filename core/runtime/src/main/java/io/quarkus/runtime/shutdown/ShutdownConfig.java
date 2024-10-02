package io.quarkus.runtime.shutdown;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Shutdown
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class ShutdownConfig {

    /**
     * The timeout to wait for running requests to finish. If this is not set then the application will exit immediately.
     *
     * Setting this timeout will incur a small performance penalty, as it requires active requests to be tracked.
     */
    @ConfigItem
    public Optional<Duration> timeout;

    /**
     * Delay between shutdown being requested and actually initiated. Also called the pre-shutdown phase.
     * In pre-shutdown, the server continues working as usual, except a readiness probe starts reporting "down"
     * (if the {@code smallrye-health} extension is present). This gives the infrastructure time to detect
     * that the application instance is shutting down and stop routing traffic to it.
     *
     * Notice that this property will only take effect if {@code quarkus.shutdown.delay-enabled} is explicitly
     * set to {@code true}.
     */
    @ConfigItem
    public Optional<Duration> delay;

    public boolean isShutdownTimeoutSet() {
        return timeout.isPresent() && timeout.get().toMillis() > 0;
    }

    public boolean isDelaySet() {
        return delay.isPresent() && delay.get().toMillis() > 0;
    }

}
