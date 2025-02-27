package io.quarkus.jaxrs.client.reactive.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * used by query param handling mechanism, in generated code
 */
@SuppressWarnings("unused")
public class ToObjectArray {

    private static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = {};
    private static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = {};
    private static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = {};
    private static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = {};
    private static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = {};
    private static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = {};
    private static final Long[] EMPTY_LONG_OBJECT_ARRAY = {};
    private static final Object[] EMPTY_OBJECT_ARRAY = {};
    private static final Short[] EMPTY_SHORT_OBJECT_ARRAY = {};

    public static Object[] collection(Collection<?> collection) {
        return collection.toArray();
    }

    public static Object[] value(Object value) {
        return new Object[] { value };
    }

    public static Object[] optional(Optional<?> optional) {
        return optional.isPresent() ? new Object[] { optional.get() } : EMPTY_OBJECT_ARRAY;
    }

    public static Boolean[] primitiveArray(final boolean[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY_BOOLEAN_OBJECT_ARRAY;
        }
        final Boolean[] result = new Boolean[array.length];
        return setAll(result, i -> array[i] ? Boolean.TRUE : Boolean.FALSE);
    }

    public static Byte[] primitiveArray(final byte[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY_BYTE_OBJECT_ARRAY;
        }
        return setAll(new Byte[array.length], new IntFunction<>() {
            @Override
            public Byte apply(int index) {
                return array[index];
            }
        });
    }

    public static Character[] primitiveArray(final char[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY_CHARACTER_OBJECT_ARRAY;
        }
        return setAll(new Character[array.length], new IntFunction<>() {
            @Override
            public Character apply(int index) {
                return array[index];
            }
        });
    }

    public static Double[] primitiveArray(final double[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        return setAll(new Double[array.length], new IntFunction<>() {
            @Override
            public Double apply(int index) {
                return array[index];
            }
        });
    }

    public static Float[] primitiveArray(final float[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY_FLOAT_OBJECT_ARRAY;
        }
        return setAll(new Float[array.length], new IntFunction<>() {
            @Override
            public Float apply(int index) {
                return array[index];
            }
        });
    }

    public static Integer[] primitiveArray(final int[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY_INTEGER_OBJECT_ARRAY;
        }
        return setAll(new Integer[array.length], new IntFunction<>() {
            @Override
            public Integer apply(int index) {
                return array[index];
            }
        });
    }

    public static Long[] primitiveArray(final long[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY_LONG_OBJECT_ARRAY;
        }
        return setAll(new Long[array.length], new IntFunction<>() {
            @Override
            public Long apply(int index) {
                return array[index];
            }
        });
    }

    public static Short[] primitiveArray(final short[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY_SHORT_OBJECT_ARRAY;
        }
        return setAll(new Short[array.length], new IntFunction<>() {
            @Override
            public Short apply(int index) {
                return array[index];
            }
        });
    }

    private static <T> T[] setAll(final T[] array, final IntFunction<? extends T> generator) {
        if (array != null && generator != null) {
            Arrays.setAll(array, generator);
        }
        return array;
    }

    private ToObjectArray() {
    }
}
