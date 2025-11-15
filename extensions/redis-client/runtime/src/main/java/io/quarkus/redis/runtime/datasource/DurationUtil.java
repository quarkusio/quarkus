package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

class DurationUtil {
    /**
     * {@return given {@code duration} converted to a number of seconds}
     * If the given {@code duration} represents a whole number of seconds, the result is a {@code Long},
     * otherwise it is a {@code Double} with millisecond precision.
     */
    static Number durationToSeconds(Duration duration) {
        if (duration.getNano() == 0) {
            return duration.getSeconds();
        } else {
            return duration.toMillis() / 1000.0;
        }
    }
}
