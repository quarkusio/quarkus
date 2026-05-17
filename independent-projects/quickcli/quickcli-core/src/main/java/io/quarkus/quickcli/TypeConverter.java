package io.quarkus.quickcli;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Converts string arguments to typed values. Supports common Java types out of the box.
 */
public final class TypeConverter {

    private static final Map<Class<?>, Function<String, Object>> CONVERTERS = new HashMap<>();

    static {
        // Primitives and wrappers
        CONVERTERS.put(String.class, s -> s);
        CONVERTERS.put(int.class, Integer::parseInt);
        CONVERTERS.put(Integer.class, Integer::valueOf);
        CONVERTERS.put(long.class, Long::parseLong);
        CONVERTERS.put(Long.class, Long::valueOf);
        CONVERTERS.put(short.class, Short::parseShort);
        CONVERTERS.put(Short.class, Short::valueOf);
        CONVERTERS.put(byte.class, Byte::parseByte);
        CONVERTERS.put(Byte.class, Byte::valueOf);
        CONVERTERS.put(float.class, Float::parseFloat);
        CONVERTERS.put(Float.class, Float::valueOf);
        CONVERTERS.put(double.class, Double::parseDouble);
        CONVERTERS.put(Double.class, Double::valueOf);
        CONVERTERS.put(boolean.class, TypeConverter::parseBoolean);
        CONVERTERS.put(Boolean.class, TypeConverter::parseBoolean);
        CONVERTERS.put(char.class, s -> s.charAt(0));
        CONVERTERS.put(Character.class, s -> s.charAt(0));

        // Common types
        CONVERTERS.put(File.class, File::new);
        CONVERTERS.put(Path.class, Path::of);
        CONVERTERS.put(BigDecimal.class, BigDecimal::new);
        CONVERTERS.put(BigInteger.class, BigInteger::new);
        CONVERTERS.put(URI.class, URI::create);
    }

    private static final Map<Class<?>, Function<String, Object>> CUSTOM_CONVERTERS = new HashMap<>();

    private TypeConverter() {
    }

    /**
     * Register a custom converter for a type.
     */
    public static <T> void register(Class<T> type, Function<String, T> converter) {
        @SuppressWarnings("unchecked")
        Function<String, Object> fn = (Function<String, Object>) (Function<String, ?>) converter;
        CUSTOM_CONVERTERS.put(type, fn);
    }

    /**
     * Convert a string value to the target type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(String value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        // Check custom converters first
        Function<String, Object> custom = CUSTOM_CONVERTERS.get(targetType);
        if (custom != null) {
            return (T) custom.apply(value);
        }

        // Built-in converters
        Function<String, Object> converter = CONVERTERS.get(targetType);
        if (converter != null) {
            return (T) converter.apply(value);
        }

        // Enum types
        if (targetType.isEnum()) {
            @SuppressWarnings("rawtypes")
            Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
            return (T) Enum.valueOf(enumType, value);
        }

        // InetAddress
        if (InetAddress.class.isAssignableFrom(targetType)) {
            try {
                return (T) InetAddress.getByName(value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot resolve address: " + value, e);
            }
        }

        throw new IllegalArgumentException(
                "No converter registered for type: " + targetType.getName()
                        + ". Register one via TypeConverter.register()");
    }

    /**
     * Check if a type has a known converter.
     */
    public static boolean hasConverter(Class<?> type) {
        return CONVERTERS.containsKey(type)
                || CUSTOM_CONVERTERS.containsKey(type)
                || type.isEnum()
                || InetAddress.class.isAssignableFrom(type);
    }

    /**
     * Check if a type is a boolean type (for flag options that don't need a value).
     */
    public static boolean isBooleanType(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    private static Boolean parseBoolean(String s) {
        if ("true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "1".equals(s)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s) || "0".equals(s)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Not a boolean value: " + s);
    }
}
