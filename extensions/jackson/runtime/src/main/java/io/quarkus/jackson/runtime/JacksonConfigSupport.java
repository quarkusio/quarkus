package io.quarkus.jackson.runtime;

public class JacksonConfigSupport {

    private final boolean failOnUnknownProperties;

    private final boolean writeDatesAsTimestamps;

    public JacksonConfigSupport(boolean failOnUnknownProperties, boolean writeDatesAsTimestamps) {
        this.failOnUnknownProperties = failOnUnknownProperties;
        this.writeDatesAsTimestamps = writeDatesAsTimestamps;
    }

    public boolean isFailOnUnknownProperties() {
        return failOnUnknownProperties;
    }

    public boolean isWriteDatesAsTimestamps() {
        return writeDatesAsTimestamps;
    }
}
