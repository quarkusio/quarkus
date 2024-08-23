package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter to support data sizes.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class MemorySizeConverter implements Converter<MemorySize>, Serializable {
    private static final Pattern MEMORY_SIZE_PATTERN = Pattern.compile("^(\\d+)([BbKkMmGgTtPpEeZzYy]?)$");
    static final BigInteger KILO_BYTES = BigInteger.valueOf(1024);
    private static final Map<String, BigInteger> MEMORY_SIZE_MULTIPLIERS;
    private static final long serialVersionUID = -1988485929047973068L;

    static {
        MEMORY_SIZE_MULTIPLIERS = new HashMap<>();
        MEMORY_SIZE_MULTIPLIERS.put("K", KILO_BYTES);
        MEMORY_SIZE_MULTIPLIERS.put("M", KILO_BYTES.pow(2));
        MEMORY_SIZE_MULTIPLIERS.put("G", KILO_BYTES.pow(3));
        MEMORY_SIZE_MULTIPLIERS.put("T", KILO_BYTES.pow(4));
        MEMORY_SIZE_MULTIPLIERS.put("P", KILO_BYTES.pow(5));
        MEMORY_SIZE_MULTIPLIERS.put("E", KILO_BYTES.pow(6));
        MEMORY_SIZE_MULTIPLIERS.put("Z", KILO_BYTES.pow(7));
        MEMORY_SIZE_MULTIPLIERS.put("Y", KILO_BYTES.pow(8));
    }

    /**
     * Convert data size configuration value respecting the following format (shown in regular expression)
     * "[0-9]+[BbKkMmGgTtPpEeZzYy]?"
     * If the value contain no suffix, the size is treated as bytes.
     *
     * @param value - value to convert.
     * @return {@link MemorySize} - a memory size represented by the given value
     */
    public MemorySize convert(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        Matcher matcher = MEMORY_SIZE_PATTERN.matcher(value);
        if (matcher.find()) {
            BigInteger number = new BigInteger(matcher.group(1));
            String scale = matcher.group(2).toUpperCase();
            BigInteger multiplier = MEMORY_SIZE_MULTIPLIERS.get(scale);
            return multiplier == null ? new MemorySize(number) : new MemorySize(number.multiply(multiplier));
        }

        throw new IllegalArgumentException(
                String.format("value %s not in correct format (regular expression): [0-9]+[BbKkMmGgTtPpEeZzYy]?", value));
    }
}
