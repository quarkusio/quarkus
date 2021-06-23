package io.quarkus.qute;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Booleans {

    private Booleans() {
    }

    /**
     * A value is considered falsy if it's null, "not found" as defined by {@link Results#isNotFound(Object)}, {code false}, an
     * empty collection, an empty map, an empty array, an empty string/char sequence or a number equal to zero.
     * 
     * @param value
     * @return {@code true} if the value is falsy
     */
    public static boolean isFalsy(Object value) {
        if (value == null || Results.isNotFound(value)) {
            return true;
        } else if (value instanceof Boolean) {
            return !(Boolean) value;
        } else if (value instanceof AtomicBoolean) {
            return !((AtomicBoolean) value).get();
        } else if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        } else if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        } else if (value instanceof CharSequence) {
            return ((CharSequence) value).length() == 0;
        } else if (value instanceof Number) {
            return isZero((Number) value);
        }
        return false;
    }

    private static boolean isZero(Number number) {
        if (number instanceof BigDecimal) {
            return BigDecimal.ZERO.compareTo((BigDecimal) number) == 0;
        } else if (number instanceof BigInteger) {
            return BigInteger.ZERO.equals(number);
        }
        if (number instanceof Float || number instanceof Double) {
            return number.doubleValue() == 0.0;
        }
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return number.longValue() == 0L;
        }
        return BigDecimal.ZERO.compareTo(new BigDecimal(number.toString())) == 0;
    }

}
