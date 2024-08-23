package io.quarkus.runtime.configuration;

import org.eclipse.microprofile.config.spi.Converter;

public class TrimmedStringConverter implements Converter<String> {

    public TrimmedStringConverter() {
    }

    @Override
    public String convert(String s) {
        if (s == null) {
            return null;
        }

        String trimmedString = s.trim();

        if (trimmedString.isEmpty()) {
            return null;
        }

        return trimmedString;
    }
}
