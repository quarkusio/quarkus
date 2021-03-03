package io.quarkus.jackson.runtime;

import java.time.ZoneId;

public class JacksonConfigSupport {

    private final boolean failOnUnknownProperties;

    private final boolean writeDatesAsTimestamps;

    private final ZoneId timeZone;

    public JacksonConfigSupport(boolean failOnUnknownProperties, boolean writeDatesAsTimestamps, ZoneId timeZone) {
        this.failOnUnknownProperties = failOnUnknownProperties;
        this.writeDatesAsTimestamps = writeDatesAsTimestamps;
        this.timeZone = timeZone;
    }

    public boolean isFailOnUnknownProperties() {
        return failOnUnknownProperties;
    }

    public boolean isWriteDatesAsTimestamps() {
        return writeDatesAsTimestamps;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }
}
