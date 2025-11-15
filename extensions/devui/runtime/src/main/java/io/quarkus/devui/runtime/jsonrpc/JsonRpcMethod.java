package io.quarkus.devui.runtime.jsonrpc;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public final class JsonRpcMethod {
    private Class bean;
    private String methodName;
    private String description;
    private Method javaMethod;
    private Map<String, Parameter> parameters;

    private List<Usage> usage;
    private boolean mcpEnabledByDefault = false;

    private RuntimeValue runtimeValue;

    private boolean isExplicitlyBlocking;
    private boolean isExplicitlyNonBlocking;

    public JsonRpcMethod() {
    }

    public Class getBean() {
        return bean;
    }

    public void setBean(Class bean) {
        this.bean = bean;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getJavaMethodName() {
        if (methodName.contains(UNDERSCORE)) {
            return methodName.substring(methodName.indexOf(UNDERSCORE) + 1);
        }
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Usage> getUsage() {
        return usage;
    }

    public void setUsage(List<Usage> usage) {
        this.usage = usage;
    }

    public boolean isMcpEnabledByDefault() {
        return mcpEnabledByDefault;
    }

    public void setMcpEnabledByDefault(boolean mcpEnabledByDefault) {
        this.mcpEnabledByDefault = mcpEnabledByDefault;
    }

    public Method getJavaMethod() {
        return javaMethod;
    }

    public void setJavaMethod(Method javaMethod) {
        this.javaMethod = javaMethod;
    }

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(String name, String description) {
        if (this.parameters == null)
            this.parameters = new LinkedHashMap<>();
        this.parameters.put(name, new Parameter(String.class, description, true));
    }

    public void addParameter(String name, String description, boolean required) {
        if (this.parameters == null)
            this.parameters = new LinkedHashMap<>();
        this.parameters.put(name, new Parameter(String.class, description, required));
    }

    public void addParameter(String name, Class<?> type, String description) {
        if (this.parameters == null)
            this.parameters = new LinkedHashMap<>();
        this.parameters.put(name, new Parameter(type, description, true));
    }

    public void addParameter(String name, Class<?> type, String description, boolean required) {
        if (this.parameters == null)
            this.parameters = new LinkedHashMap<>();
        this.parameters.put(name, new Parameter(type, description, required));
    }

    public boolean hasParameters() {
        return this.parameters != null && !this.parameters.isEmpty();
    }

    public RuntimeValue getRuntimeValue() {
        return runtimeValue;
    }

    public void setRuntimeValue(RuntimeValue runtimeValue) {
        this.runtimeValue = runtimeValue;
    }

    public boolean isIsExplicitlyBlocking() {
        return isExplicitlyBlocking;
    }

    public void setIsExplicitlyBlocking(boolean isExplicitlyBlocking) {
        this.isExplicitlyBlocking = isExplicitlyBlocking;
    }

    public boolean isIsExplicitlyNonBlocking() {
        return isExplicitlyNonBlocking;
    }

    public void setIsExplicitlyNonBlocking(boolean isExplicitlyNonBlocking) {
        this.isExplicitlyNonBlocking = isExplicitlyNonBlocking;
    }

    public boolean isReturningMulti() {
        return javaMethod.getReturnType().getName().equals(Multi.class.getName());
    }

    public boolean isReturningUni() {
        return javaMethod.getReturnType().getName().equals(Uni.class.getName());
    }

    public boolean isReturningCompletionStage() {
        return javaMethod.getReturnType().getName().equals(CompletionStage.class.getName());
    }

    public boolean isReturningCompletableFuture() {
        return javaMethod.getReturnType().getName().equals(CompletableFuture.class.getName());
    }

    public static class Parameter {
        private Class<?> type;
        private String description;
        private boolean required;

        public Parameter(Class<?> type, String description, boolean required) {
            this.type = type;
            this.description = description;
            this.required = required;
        }

        public Class<?> getType() {
            return type;
        }

        public void setType(Class<?> type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

    }

    private static final String UNDERSCORE = "_";

}
