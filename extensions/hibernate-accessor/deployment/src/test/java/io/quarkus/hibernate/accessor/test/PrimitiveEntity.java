package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class PrimitiveEntity {

    @ReflectionFreeAccessor
    private boolean booleanValue;

    @ReflectionFreeAccessor
    private byte byteValue;

    @ReflectionFreeAccessor
    private char charValue;

    @ReflectionFreeAccessor
    private short shortValue;

    @ReflectionFreeAccessor
    private int intValue;

    @ReflectionFreeAccessor
    private long longValue;

    @ReflectionFreeAccessor
    private float floatValue;

    @ReflectionFreeAccessor
    private double doubleValue;

    public boolean isBooleanValue() {
        return booleanValue;
    }

    public byte getByteValue() {
        return byteValue;
    }

    public char getCharValue() {
        return charValue;
    }

    public short getShortValue() {
        return shortValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public float getFloatValue() {
        return floatValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }
}
