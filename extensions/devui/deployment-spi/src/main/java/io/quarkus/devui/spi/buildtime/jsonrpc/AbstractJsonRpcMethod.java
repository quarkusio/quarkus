package io.quarkus.devui.spi.buildtime.jsonrpc;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.runtime.annotations.Usage;

/**
 * Base class for json-rpc methods
 */
public abstract class AbstractJsonRpcMethod {

    private String methodName;
    private String description;
    private Map<String, Parameter> parameters;
    private EnumSet<Usage> usage;
    private boolean mcpEnabledByDefault = false;

    public AbstractJsonRpcMethod() {
    }

    public AbstractJsonRpcMethod(String methodName, String description,
            EnumSet<Usage> usage, boolean mcpEnabledByDefault) {
        this.methodName = methodName;
        this.description = description;
        this.usage = usage;
        this.mcpEnabledByDefault = mcpEnabledByDefault;
    }

    public AbstractJsonRpcMethod(String methodName, String description, Map<String, Parameter> parameters,
            EnumSet<Usage> usage, boolean mcpEnabledByDefault) {
        this.methodName = methodName;
        this.description = description;
        this.parameters = parameters;
        this.usage = usage;
        this.mcpEnabledByDefault = mcpEnabledByDefault;
    }

    public String getMethodName() {
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

    public EnumSet<Usage> getUsage() {
        return usage;
    }

    public void setUsage(EnumSet<Usage> usage) {
        this.usage = usage;
    }

    public boolean isMcpEnabledByDefault() {
        return mcpEnabledByDefault;
    }

    public void setMcpEnabledByDefault(boolean mcpEnabledByDefault) {
        this.mcpEnabledByDefault = mcpEnabledByDefault;
    }

    public static class Parameter {
        private Class<?> type;
        private String description;
        private boolean required;

        public Parameter() {

        }

        public Parameter(Class<?> type, String description, boolean required) {
            this.type = type;
            this.description = description;
            this.required = required;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
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
    }
}