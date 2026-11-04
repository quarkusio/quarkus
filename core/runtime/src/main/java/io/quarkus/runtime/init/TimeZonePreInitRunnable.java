package io.quarkus.runtime.init;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Pre-initializes the time zone and the {@code java.time} machinery the first log line needs.
 * <p>
 * The console handler builds a {@link DateTimeFormatter} for the {@code %d} pattern step and the first record formats an
 * {@link Instant} in the default zone, which triggers the {@code DateTimeFormatter} / {@code DateTimeFormatterBuilder}
 * static initialization, the printer classes and the computation of the zone's transitions for the current year. The
 * pattern used here is the default one of the Quarkus console handler; only the class initialization and the zone rules
 * matter, so a different user pattern still benefits.
 */
public class TimeZonePreInitRunnable implements Runnable {

    @Override
    public void run() {
        ZoneId zoneId = TimeZone.getDefault().toZoneId();
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS").formatTo(Instant.now().atZone(zoneId), new StringBuilder());
    }
}
