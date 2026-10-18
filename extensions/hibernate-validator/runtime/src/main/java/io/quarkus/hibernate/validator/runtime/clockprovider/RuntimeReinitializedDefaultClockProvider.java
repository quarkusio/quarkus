package io.quarkus.hibernate.validator.runtime.clockprovider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import jakarta.validation.ClockProvider;

/**
 * A Quarkus-specific clock provider that can provide a clock based on a runtime system time zone.
 */
public class RuntimeReinitializedDefaultClockProvider implements ClockProvider {

    public static final RuntimeReinitializedDefaultClockProvider INSTANCE = new RuntimeReinitializedDefaultClockProvider();

    private static final RuntimeReinitializedDefaultClock clock = new RuntimeReinitializedDefaultClock();

    private RuntimeReinitializedDefaultClockProvider() {
    }

    @Override
    public Clock getClock() {
        return clock;
    }

    private static class RuntimeReinitializedDefaultClock extends Clock {

        @Override
        public ZoneId getZone() {
            // we delegate getting the zone id value to a helper class that is reinitialized at runtime
            // allowing to pick up an actual runtime timezone.
            return HibernateValidatorClockProviderSystemZoneIdHolder.SYSTEM_ZONE_ID;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.system(zone);
        }

        @Override
        public Instant instant() {
            return Instant.now();
        }
    }
}
