package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class PrimitiveEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private boolean booleanValue;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private byte byteValue;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private char charValue;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private short shortValue;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private int intValue;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private long longValue;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private float floatValue;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
