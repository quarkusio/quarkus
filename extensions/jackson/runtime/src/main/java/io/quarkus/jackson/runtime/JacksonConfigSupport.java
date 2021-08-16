package io.quarkus.jackson.runtime;

import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonInclude;

public class JacksonConfigSupport {

    private final boolean failOnUnknownProperties;

    private final boolean failOnEmptyBeans;

    private final boolean writeDatesAsTimestamps;

    private final boolean acceptCaseInsensitiveEnums;

    private final ZoneId timeZone;

    private JsonInclude.Include serializationInclusion;

    public JacksonConfigSupport(boolean failOnUnknownProperties, boolean failOnEmptyBeans, boolean writeDatesAsTimestamps,
            boolean acceptCaseInsensitiveEnums, ZoneId timeZone, JsonInclude.Include serializationInclusion) {
        this.failOnUnknownProperties = failOnUnknownProperties;
        this.failOnEmptyBeans = failOnEmptyBeans;
        this.writeDatesAsTimestamps = writeDatesAsTimestamps;
        this.acceptCaseInsensitiveEnums = acceptCaseInsensitiveEnums;
        this.timeZone = timeZone;
        this.serializationInclusion = serializationInclusion;
    }

    public boolean isFailOnUnknownProperties() {
        return failOnUnknownProperties;
    }

    public boolean isFailOnEmptyBeans() {
        return failOnEmptyBeans;
    }

    public boolean isWriteDatesAsTimestamps() {
        return writeDatesAsTimestamps;
    }

    public boolean isAcceptCaseInsensitiveEnums() {
        return acceptCaseInsensitiveEnums;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public JsonInclude.Include getSerializationInclusion() {
        return serializationInclusion;
    }
}
