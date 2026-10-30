package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class PrimitiveEntity {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private boolean booleanValue;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private byte byteValue;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private char charValue;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private short shortValue;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private int intValue;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private long longValue;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private float floatValue;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
