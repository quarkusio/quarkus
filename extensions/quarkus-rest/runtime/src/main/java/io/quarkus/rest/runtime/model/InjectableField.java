package io.quarkus.rest.runtime.model;

import java.util.function.Supplier;

import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverter;

public class InjectableField {
    //not the field name, the value of the name in the annotation
    public String parameterName;
    public String type;
    public ParameterType parameterType;
    private boolean single;
    public Supplier<ParameterConverter> converter;
    private String defaultValue;
    private String accessorClass;

    public InjectableField() {
    }

    public InjectableField(String parameterName, String type, ParameterType parameterType, boolean single,
            Supplier<ParameterConverter> converter, String defaultValue, String accessorClass) {
        this.parameterName = parameterName;
        this.type = type;
        this.converter = converter;
        this.parameterType = parameterType;
        this.single = single;
        this.defaultValue = defaultValue;
        this.accessorClass = accessorClass;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
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

    public void setConverter(Supplier<ParameterConverter> converter) {
        this.converter = converter;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isSingle() {
        return single;
    }

    public void setSingle(boolean single) {
        this.single = single;
    }

    public String getAccessorClass() {
        return accessorClass;
    }

    public void setAccessorClass(String accessorClass) {
        this.accessorClass = accessorClass;
    }

    @Override
    public String toString() {
        return "MethodParameter{" +
                "name='" + parameterName + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
