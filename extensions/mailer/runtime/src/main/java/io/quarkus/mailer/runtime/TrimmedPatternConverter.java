package io.quarkus.mailer.runtime;

import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;

public class TrimmedPatternConverter implements Converter<Pattern> {

    public TrimmedPatternConverter() {
    }

    @Override
    public Pattern convert(String s) {
        if (s == null) {
            return null;
        }

        String trimmedString = s.trim().toLowerCase();

        if (trimmedString.isEmpty()) {
            return null;
        }

        return Pattern.compile(trimmedString, Pattern.CASE_INSENSITIVE);
    }
}
