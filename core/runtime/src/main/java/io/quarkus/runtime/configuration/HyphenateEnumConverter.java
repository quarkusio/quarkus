package io.quarkus.runtime.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter for hyphenated enums
 */
final public class HyphenateEnumConverter<E extends Enum<E>> implements Converter<E> {
    private static final String HYPHEN = "-";
    private static final Pattern PATTERN = Pattern.compile("([-_]+)");

    private final Class<E> enumType;
    private final Map<String, E> values = new HashMap<>();

    public HyphenateEnumConverter(Class<E> enumType) {
        this.enumType = enumType;

        for (E enumValue : this.enumType.getEnumConstants()) {
            final String name = enumValue.name();
            final String canonicalEquivalent = hyphenate(name);
            values.put(canonicalEquivalent, enumValue);
        }
    }

    @Override
    public E convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        final String hyphenatedValue = hyphenate(value);
        final Enum<?> enumValue = values.get(hyphenatedValue);

        if (enumValue != null) {
            return enumType.cast(enumValue);
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
