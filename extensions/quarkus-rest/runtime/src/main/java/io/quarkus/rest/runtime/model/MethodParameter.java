package io.quarkus.rest.runtime.model;

import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverterSupplier;

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
    public ParameterConverterSupplier converter;
    private String defaultValue;
    private boolean isObtainedAsCollection;

    public MethodParameter() {
    }

    public MethodParameter(String name, String type, String declaredType, ParameterType parameterType, boolean single,
            ParameterConverterSupplier converter, String defaultValue, boolean isObtainedAsCollection) {
        this.name = name;
        this.type = type;
        this.converter = converter;
        this.parameterType = parameterType;
        this.single = single;
        this.defaultValue = defaultValue;
        this.declaredType = declaredType;
        this.isObtainedAsCollection = isObtainedAsCollection;
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

    public ParameterConverterSupplier getConverter() {
        return converter;
    }

    public MethodParameter setConverter(ParameterConverterSupplier converter) {
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

    public boolean isObtainedAsCollection() {
        return isObtainedAsCollection;
    }

    public MethodParameter setObtainedAsCollection(boolean isObtainedAsCollection) {
        this.isObtainedAsCollection = isObtainedAsCollection;
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
