package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class PrimitiveTypesBean {

    private char charPrimitive;
    private Character characterPrimitive;

    private short shortPrimitive;
    private Short shortInstance;

    private int intPrimitive;
    private Integer integerInstance;

    private long longPrimitive;
    private Long longInstance;

    private float floatPrimitive;
    private Float floatInstance;

    private double doublePrimitive;
    private Double doubleInstance;

    private boolean booleanPrimitive;
    private Boolean booleanInstance;

    public PrimitiveTypesBean(char charPrimitive, Character characterPrimitive, short shortPrimitive, Short shortInstance,
            int intPrimitive, Integer integerInstance, long longPrimitive, Long longInstance, float floatPrimitive,
            Float floatInstance, double doublePrimitive, Double doubleInstance, boolean booleanPrimitive,
            Boolean booleanInstance) {
        this.charPrimitive = charPrimitive;
        this.characterPrimitive = characterPrimitive;
        this.shortPrimitive = shortPrimitive;
        this.shortInstance = shortInstance;
        this.intPrimitive = intPrimitive;
        this.integerInstance = integerInstance;
        this.longPrimitive = longPrimitive;
        this.longInstance = longInstance;
        this.floatPrimitive = floatPrimitive;
        this.floatInstance = floatInstance;
        this.doublePrimitive = doublePrimitive;
        this.doubleInstance = doubleInstance;
        this.booleanPrimitive = booleanPrimitive;
        this.booleanInstance = booleanInstance;
    }

    public char getCharPrimitive() {
        return charPrimitive;
    }

    public void setCharPrimitive(char charPrimitive) {
        this.charPrimitive = charPrimitive;
    }

    public Character getCharacterPrimitive() {
        return characterPrimitive;
    }

    public void setCharPrimitive(Character characterPrimitive) {
        this.characterPrimitive = characterPrimitive;
    }

    public short getShortPrimitive() {
        return shortPrimitive;
    }

    public void setShortPrimitive(short shortPrimitive) {
        this.shortPrimitive = shortPrimitive;
    }

    public Short getShortInstance() {
        return shortInstance;
    }

    public void setShortInstance(Short shortInstance) {
        this.shortInstance = shortInstance;
    }

    public int getIntPrimitive() {
        return intPrimitive;
    }

    public void setIntPrimitive(int intPrimitive) {
        this.intPrimitive = intPrimitive;
    }

    public Integer getIntegerInstance() {
        return integerInstance;
    }

    public void setIntegerInstance(Integer integerInstance) {
        this.integerInstance = integerInstance;
    }

    public long getLongPrimitive() {
        return longPrimitive;
    }

    public void setLongPrimitive(long longPrimitive) {
        this.longPrimitive = longPrimitive;
    }

    public Long getLongInstance() {
        return longInstance;
    }

    public void setLongInstance(Long longInstance) {
        this.longInstance = longInstance;
    }

    public float getFloatPrimitive() {
        return floatPrimitive;
    }

    public void setFloatPrimitive(float floatPrimitive) {
        this.floatPrimitive = floatPrimitive;
    }

    public Float getFloatInstance() {
        return floatInstance;
    }

    public void setFloatInstance(Float floatInstance) {
        this.floatInstance = floatInstance;
    }

    public double getDoublePrimitive() {
        return doublePrimitive;
    }

    public void setDoublePrimitive(double doublePrimitive) {
        this.doublePrimitive = doublePrimitive;
    }

    public Double getDoubleInstance() {
        return doubleInstance;
    }

    public void setDoubleInstance(Double doubleInstance) {
        this.doubleInstance = doubleInstance;
    }

    public boolean isBooleanPrimitive() {
        return booleanPrimitive;
    }

    public void setBooleanPrimitive(boolean booleanPrimitive) {
        this.booleanPrimitive = booleanPrimitive;
    }

    public Boolean getBooleanInstance() {
        return booleanInstance;
    }

    public void setBooleanInstance(Boolean booleanInstance) {
        this.booleanInstance = booleanInstance;
    }
}
