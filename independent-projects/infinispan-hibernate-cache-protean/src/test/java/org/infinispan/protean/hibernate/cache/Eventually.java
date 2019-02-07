package org.infinispan.protean.hibernate.cache;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

final class Eventually {

    private Eventually() {
    }

    static void eventually(Runnable condition) {
        final long timeout = Duration.ofSeconds(10).toMillis();
        final long pollInterval = Duration.ofMillis(500).toMillis();
        final TimeUnit unit = TimeUnit.MILLISECONDS;

        if (pollInterval <= 0) {
            throw new IllegalArgumentException("Check interval must be positive");
        }
        try {
            long expectedEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, unit);
            long sleepMillis = TimeUnit.MILLISECONDS.convert(pollInterval, unit);
            AssertionError lastError = new AssertionError("");
            while (expectedEndTime - System.nanoTime() > 0) {
                try {
                    condition.run();
                    return;
                } catch (AssertionError error) {
                    lastError = error;
                }

                Thread.sleep(sleepMillis);
            }

            throw lastError;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected!", e);
        }
    }

}
