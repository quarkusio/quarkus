package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.DurationUtil.durationToSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class DurationUtilTest {
    @Test
    public void longDuration() {
        assertEquals(0L, durationToSeconds(Duration.ofSeconds(0)));
        assertEquals(0L, durationToSeconds(Duration.ofMillis(0)));
        assertEquals(0L, durationToSeconds(Duration.ofNanos(0)));

        assertEquals(1L, durationToSeconds(Duration.ofSeconds(1)));
        assertEquals(1L, durationToSeconds(Duration.ofMillis(1_000)));
        assertEquals(1L, durationToSeconds(Duration.ofNanos(1_000_000_000)));

        assertEquals(10L, durationToSeconds(Duration.ofSeconds(10)));
        assertEquals(10L, durationToSeconds(Duration.ofMillis(10_000)));
        assertEquals(10L, durationToSeconds(Duration.ofNanos(10_000_000_000L)));
    }

    @Test
    public void doubleDuration() {
        assertEquals(0.5, durationToSeconds(Duration.ofSeconds(0, 500_000_000)));
        assertEquals(0.5, durationToSeconds(Duration.ofMillis(500)));
        assertEquals(0.5, durationToSeconds(Duration.ofNanos(500_000_000)));

        assertEquals(1.5, durationToSeconds(Duration.ofSeconds(1, 500_000_000)));
        assertEquals(1.5, durationToSeconds(Duration.ofMillis(1_500)));
        assertEquals(1.5, durationToSeconds(Duration.ofNanos(1_500_000_000)));
    }
}
