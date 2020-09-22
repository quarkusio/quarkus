package io.quarkus.rest.runtime.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverter;

public class MethodParameter {
    public String name;
    public String type;
    /**
     * Generally this will be the same as type, unless the parameter is a
     * collection, in which case 'type' will be the element type and this
     * will be the collection type
     */
    public String declaredType;
    public ParameterType parameterType;
    private boolean single;
    public Supplier<ParameterConverter> converter;
    private String defaultValue;
    private final List<InjectableField> injectableFields = new ArrayList<>();

    public MethodParameter() {
    }

    public MethodParameter(String name, String type, String declaredType, ParameterType parameterType, boolean single,
            Supplier<ParameterConverter> converter, String defaultValue) {
        this.name = name;
        this.type = type;
        this.converter = converter;
        this.parameterType = parameterType;
        this.single = single;
        this.defaultValue = defaultValue;
        this.declaredType = declaredType;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public MethodParameter setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean isSingle() {
        return single;
    }

    public MethodParameter setSingle(boolean single) {
        this.single = single;
        return this;
    }

    public String getDeclaredType() {
        return declaredType;
    }

    public MethodParameter setDeclaredType(String declaredType) {
        this.declaredType = declaredType;
        return this;
    }

    public List<InjectableField> getInjectableFields() {
        return injectableFields;
    }

    @Override
    public String toString() {
        return "MethodParameter{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
