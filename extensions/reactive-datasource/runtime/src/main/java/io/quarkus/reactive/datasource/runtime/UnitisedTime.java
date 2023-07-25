package io.quarkus.reactive.datasource.runtime;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class UnitisedTime {

    public final int value;
    public final TimeUnit unit;

    public UnitisedTime(int value, TimeUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    /**
     * Convert a {@link Duration} to a {@link UnitisedTime} with the smallest possible
     * {@link TimeUnit} starting from {@link TimeUnit#MILLISECONDS}.
     *
     * @param duration Duration to convert
     *
     * @return UnitisedTime
     */
    public static UnitisedTime unitised(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration cannot be negative.");
        }

        long millis = duration.toMillis();
        if (millis < Integer.MAX_VALUE) {
            return new UnitisedTime((int) millis, TimeUnit.MILLISECONDS);
        }

        long seconds = duration.getSeconds();
        if (seconds < Integer.MAX_VALUE) {
            return new UnitisedTime((int) seconds, TimeUnit.SECONDS);
        }

        long minutes = duration.toMinutes();
        if (minutes < Integer.MAX_VALUE) {
            return new UnitisedTime((int) minutes, TimeUnit.MINUTES);
        }

        long hours = duration.toHours();
        if (hours < Integer.MAX_VALUE) {
            return new UnitisedTime((int) hours, TimeUnit.HOURS);
        }

        long days = duration.toDays();
        return new UnitisedTime((int) days, TimeUnit.DAYS);

    }
}
