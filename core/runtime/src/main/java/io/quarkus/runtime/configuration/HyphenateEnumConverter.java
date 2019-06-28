package io.quarkus.runtime.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter for hyphenated enums
 */
final public class HyphenateEnumConverter implements Converter<Enum<?>> {
    private static final String HYPHEN = "-";
    private static final Pattern PATTERN = Pattern.compile("([-_]+)");

    private final Class<? extends Enum<?>> enumType;
    private final Map<String, Enum<?>> HYPHENATED_ENUM = new HashMap<>();

    public HyphenateEnumConverter(Class<? extends Enum<?>> enumType) {
        this.enumType = enumType;

        for (Enum enumValue : this.enumType.getEnumConstants()) {
            final String name = enumValue.name();
            final String canonicalEquivalent = hyphenate(name);
            this.HYPHENATED_ENUM.put(canonicalEquivalent, enumValue);
        }
    }

    @Override
    public Enum<?> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        final String hyphenatedValue = hyphenate(value);
        final Enum<?> enumValue = HYPHENATED_ENUM.get(hyphenatedValue);

        if (enumValue != null) {
            return enumValue;
        }

        throw new IllegalArgumentException(String.format("Cannot convert %s to enum %s", value, enumType));
    }

    private String hyphenate(String value) {
        StringBuffer target = new StringBuffer();
        String hyphenate = io.quarkus.runtime.util.StringUtil.hyphenate(value);
        Matcher matcher = PATTERN.matcher(hyphenate);
        while (matcher.find()) {
            matcher.appendReplacement(target, HYPHEN);
        }
        matcher.appendTail(target);
        return target.toString();
    }
}
