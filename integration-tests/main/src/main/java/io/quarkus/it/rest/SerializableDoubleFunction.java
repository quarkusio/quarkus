package io.quarkus.it.rest;

import java.io.Serializable;
import java.util.function.ToDoubleFunction;

class SerializableDoubleFunction implements Serializable, ToDoubleFunction<Integer> {
    private final double value;

    public SerializableDoubleFunction(double inputValue) {
        value = inputValue;
    }

    @Override
    public double applyAsDouble(Integer o) {
        return value;
    }
}
