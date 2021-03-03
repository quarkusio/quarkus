package io.quarkus.jackson.runtime;

public class JacksonConfigSupport {

    private final boolean failOnUnknownProperties;

    private final boolean writeDatesAsTimestamps;

    private final String timeZone;

    public JacksonConfigSupport(boolean failOnUnknownProperties, boolean writeDatesAsTimestamps, String timeZone) {
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

    public String getTimeZone() {
        return timeZone;
    }
}
