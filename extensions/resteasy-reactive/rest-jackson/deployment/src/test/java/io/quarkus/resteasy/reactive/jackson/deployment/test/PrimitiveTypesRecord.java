package io.quarkus.resteasy.reactive.jackson.deployment.test;

public record PrimitiveTypesRecord(char charPrimitive, Character characterPrimitive, short shortPrimitive, Short shortInstance,
        int intPrimitive, Integer integerInstance, long longPrimitive, Long longInstance, float floatPrimitive,
        Float floatInstance, double doublePrimitive, Double doubleInstance, boolean booleanPrimitive, Boolean booleanInstance) {
}
