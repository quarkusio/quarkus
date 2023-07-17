package io.quarkus.runtime.shutdown;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class ShutdownConfig {

    /**
     * The timeout to wait for running requests to finish. If this is not set then the application will exit immediately.
     *
     * Setting this timeout will incur a small performance penalty, as it requires active requests to be tracked.
     */
    @ConfigItem
    public Optional<Duration> timeout;

    public boolean isShutdownTimeoutSet() {
        return timeout.isPresent() && timeout.get().toMillis() > 0;
    }

}
