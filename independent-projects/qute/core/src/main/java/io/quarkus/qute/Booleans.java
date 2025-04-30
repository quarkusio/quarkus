package io.quarkus.qute;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
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
        } else if (value instanceof Boolean bool) {
            return !bool;
        } else if (value instanceof AtomicBoolean atomicBool) {
            return !atomicBool.get();
        } else if (value instanceof Collection col) {
            return col.isEmpty();
        } else if (value instanceof Map map) {
            return map.isEmpty();
        } else if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        } else if (value instanceof CharSequence cs) {
            return cs.length() == 0;
        } else if (value instanceof Number num) {
            return isZero(num);
        } else if (value instanceof Optional opt) {
            return opt.isEmpty();
        } else if (value instanceof OptionalInt opt) {
            return opt.isEmpty();
        } else if (value instanceof OptionalLong opt) {
            return opt.isEmpty();
        } else if (value instanceof OptionalDouble opt) {
            return opt.isEmpty();
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
