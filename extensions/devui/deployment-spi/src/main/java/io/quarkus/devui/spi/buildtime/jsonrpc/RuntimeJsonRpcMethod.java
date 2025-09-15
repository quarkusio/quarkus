package io.quarkus.devui.spi.buildtime.jsonrpc;

import java.util.EnumSet;
import java.util.Map;

import io.quarkus.runtime.annotations.Usage;

/**
 * Runtime json-rpc methods. Here we need to know the CDI bean to call
 */
public final class RuntimeJsonRpcMethod extends AbstractJsonRpcMethod {

    private Class<?> bean;
    private boolean blocking;
    private boolean nonBlocking;

    public RuntimeJsonRpcMethod() {
        super();
    }

    public RuntimeJsonRpcMethod(String methodName,
            String description,
            Map<String, Parameter> parameters,
            EnumSet<Usage> usage,
            boolean mcpEnabledByDefault,
            Class<?> bean,
            boolean blocking,
            boolean nonBlocking) {
        super(methodName, description, parameters, usage, mcpEnabledByDefault);
        this.bean = bean;
        this.blocking = blocking;
        this.nonBlocking = nonBlocking;
    }

    public Class<?> getBean() {
        return bean;
    }

    public void setBean(Class<?> bean) {
        this.bean = bean;
    }

    public boolean isExplicitlyBlocking() {
        return blocking;
    }

    public void setExplicitlyBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public boolean isExplicitlyNonBlocking() {
        return nonBlocking;
    }

    public void setExplicitlyNonBlocking(boolean nonBlocking) {
        this.nonBlocking = nonBlocking;
    }
}