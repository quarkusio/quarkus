package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.util.Optional;

public class GlobalSerializationConfig {

    private final Optional<String> locale;
    private final Optional<String> dateFormat;
    private final boolean serializeNullValues;
    private final String propertyOrderStrategy;

    public GlobalSerializationConfig(Optional<String> locale, Optional<String> dateFormat, boolean serializeNullValues,
            String propertyOrderStrategy) {
        this.locale = locale;
        this.dateFormat = dateFormat;
        this.serializeNullValues = serializeNullValues;
        this.propertyOrderStrategy = propertyOrderStrategy;
    }

    public Optional<String> getLocale() {
        return locale;
    }

    public Optional<String> getDateFormat() {
        return dateFormat;
    }

    public boolean isSerializeNullValues() {
        return serializeNullValues;
    }

    public String getPropertyOrderStrategy() {
        return propertyOrderStrategy;
    }
}
