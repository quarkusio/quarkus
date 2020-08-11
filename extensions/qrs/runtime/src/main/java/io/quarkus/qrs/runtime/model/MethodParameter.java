package io.quarkus.qrs.runtime.model;

import java.util.function.Supplier;

import io.quarkus.qrs.runtime.core.parameters.converters.ParameterConverter;

public class MethodParameter {
    public String name;
    public String type;
    public ParameterType parameterType;
    private boolean single;
    public Supplier<ParameterConverter> converter;

    public MethodParameter() {
    }

    public MethodParameter(String name, String type, ParameterType parameterType, boolean single,
            Supplier<ParameterConverter> converter) {
        this.name = name;
        this.type = type;
        this.converter = converter;
        this.parameterType = parameterType;
        this.single = single;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ParameterType getParameterType() {
        return parameterType;
    }

    public void setParameterType(ParameterType parameterType) {
        this.parameterType = parameterType;
    }

    public Supplier<ParameterConverter> getConverter() {
        return converter;
    }

    public MethodParameter setConverter(Supplier<ParameterConverter> converter) {
        this.converter = converter;
        return this;
    }

    public boolean isSingle() {
        return single;
    }

    public MethodParameter setSingle(boolean single) {
        this.single = single;
        return this;
    }

    @Override
    public String toString() {
        return "MethodParameter{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
