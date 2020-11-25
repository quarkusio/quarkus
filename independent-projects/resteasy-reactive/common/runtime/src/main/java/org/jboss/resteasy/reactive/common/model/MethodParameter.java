package org.jboss.resteasy.reactive.common.model;

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
    public boolean encoded;
    private boolean single;
    private String defaultValue;
    private boolean isObtainedAsCollection;

    public MethodParameter() {
    }

    public MethodParameter(String name, String type, String declaredType, ParameterType parameterType, boolean single,
            String defaultValue, boolean isObtainedAsCollection, boolean encoded) {
        this.name = name;
        this.type = type;
        this.parameterType = parameterType;
        this.single = single;
        this.defaultValue = defaultValue;
        this.declaredType = declaredType;
        this.isObtainedAsCollection = isObtainedAsCollection;
        this.encoded = encoded;
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
