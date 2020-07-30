package io.quarkus.funqy.test;

import java.time.OffsetDateTime;

public class Simple {
    private String value;
    private int intVal;
    private short shortVal;
    private long longVal;
    private byte b;
    private float floatVal;
    private double doubleVal;
    private boolean boolVal;
    private OffsetDateTime time;

    public OffsetDateTime getTime() {
        return time;
    }

    public void setTime(OffsetDateTime time) {
        this.time = time;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getIntVal() {
        return intVal;
    }

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public short getShortVal() {
        return shortVal;
    }

    public void setShortVal(short shortVal) {
        this.shortVal = shortVal;
    }

    public long getLongVal() {
        return longVal;
    }

    public void setLongVal(long longVal) {
        this.longVal = longVal;
    }

    public byte getB() {
        return b;
    }

    public void setB(byte b) {
        this.b = b;
    }

    public float getFloatVal() {
        return floatVal;
    }

    public void setFloatVal(float floatVal) {
        this.floatVal = floatVal;
    }

    public double getDoubleVal() {
        return doubleVal;
    }

    public void setDoubleVal(double doubleVal) {
        this.doubleVal = doubleVal;
    }

    public boolean isBoolVal() {
        return boolVal;
    }

    public void setBoolVal(boolean boolVal) {
        this.boolVal = boolVal;
    }
}
