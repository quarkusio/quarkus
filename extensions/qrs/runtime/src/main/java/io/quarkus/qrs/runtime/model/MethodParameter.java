package io.quarkus.qrs.runtime.model;

public class MethodParameter {
    public String name;
    public String type;
    public ParameterType parameterType;

    public MethodParameter(String name, String type, ParameterType parameterType) {
        this.name = name;
        this.type = type;
        this.parameterType = parameterType;
    }

    public MethodParameter() {

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
}
