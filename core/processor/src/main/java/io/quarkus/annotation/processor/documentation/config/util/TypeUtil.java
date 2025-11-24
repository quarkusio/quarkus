package io.quarkus.annotation.processor.documentation.config.util;

import java.util.Locale;

public final class TypeUtil {

    /*
     * Retrieve a default value of a primitive type.
     *
     */
    public static String getPrimitiveDefaultValue(String primitiveType) {
        return Types.PRIMITIVE_DEFAULT_VALUES.get(primitiveType);
    }

    /**
     * Replaces Java primitive wrapper types with primitive types
     */
    public static String unbox(String type) {
        String mapping = Types.WRAPPERS_TO_PRIMITIVES.get(type);
        return mapping == null ? type : mapping;
    }

    public static boolean isPrimitiveWrapper(String type) {
        return Types.WRAPPERS_TO_PRIMITIVES.containsKey(type);
    }

    public static String primitiveToWrapper(String type) {
        if (Types.PRIMITIVES_TO_WRAPPERS.containsKey(type)) {
            return Types.PRIMITIVES_TO_WRAPPERS.get(type);
        }

        return type;
    }

    public static String getAlias(String qualifiedName) {
        return Types.ALIASED_TYPES.get(qualifiedName);
    }

    public static String normalizeDurationValue(String value) {
        if (!value.isEmpty() && Character.isDigit(value.charAt(value.length() - 1))) {
            try {
                value = Integer.parseInt(value) + "S";
            } catch (NumberFormatException ignore) {
            }
        }
        return value.toUpperCase(Locale.ROOT);
    }
}
