package io.quarkus.redis.runtime.datasource;

import java.util.Collection;
import java.util.Map;

public class Validation {

    private Validation() {
        // avoid direct instantiation
    }

    public static <X> X[] notNullOrEmpty(X[] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("`" + name + "` must not be empty");
        }
        return array;
    }

    public static float[] notNullOrEmpty(float[] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("`" + name + "` must not be empty");
        }
        return array;
    }

    public static double[] notNullOrEmpty(double[] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("`" + name + "` must not be empty");
        }
        return array;
    }

    public static int[] notNullOrEmpty(int[] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("`" + name + "` must not be empty");
        }
        return array;
    }

    public static long[] notNullOrEmpty(long[] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("`" + name + "` must not be empty");
        }
        return array;
    }

    public static byte[] notNullOrEmpty(byte[] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("`" + name + "` must not be empty");
        }
        return array;
    }

    public static String notNullOrBlank(String v, String name) {
        if (v == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (v.isBlank()) {
            throw new IllegalArgumentException("`" + name + "` must not be blank");
        }
        return v;
    }

    static <X> void notNullOrEmpty(Collection<X> col, String name) {
        if (col == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (col.size() == 0) {
            throw new IllegalArgumentException("`" + name + "` must not be empty");
        }
    }

    static <K, V> void notNullOrEmpty(Map<K, V> map, String name) {
        if (map == null) {
            throw new IllegalArgumentException("`" + name + "` must not be `null`");
        }
        if (map.size() == 0) {
            throw new IllegalArgumentException("`" + name + "` must not be empty");
        }
    }

    static void validateLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("The longitude must be in [-180, 180]");
        }
    }

    static void validateLatitude(double latitude) {
        if (latitude < -85.05112878 || latitude > 85.05112878) {
            throw new IllegalArgumentException("The latitude must be in [85.05112878, 85.05112878]");
        }
    }

    public static void positive(double amount, String name) {
        if (amount <= 0) {
            throw new IllegalArgumentException(String.format("`%s` must be greater than zero`", name));
        }
    }

    public static void positiveOrZero(double amount, String name) {
        if (amount < 0) {
            throw new IllegalArgumentException(String.format("`%s` must be greater or equal to zero`", name));
        }
    }

    public static void isBit(int b, String name) {
        if (b != 0 && b != 1) {
            throw new IllegalArgumentException(String.format("%s` must be either `0` or `1`", name));
        }
    }

}
