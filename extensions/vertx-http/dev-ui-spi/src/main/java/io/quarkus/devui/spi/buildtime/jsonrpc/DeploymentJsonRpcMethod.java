package io.quarkus.devui.spi.buildtime.jsonrpc;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.runtime.annotations.Usage;

/**
 * Deployment json-rpc methods. Here we need to know the function to call
 */
public final class DeploymentJsonRpcMethod extends AbstractJsonRpcMethod {

    private Function<Map<String, String>, ?> action;

    public DeploymentJsonRpcMethod() {
    }

    public DeploymentJsonRpcMethod(String methodName,
            String description,
            EnumSet<Usage> usage,
            Function<Map<String, String>, ?> action) {
        super(methodName, description, usage);
        this.action = action;
    }

    public DeploymentJsonRpcMethod(String methodName,
            String description,
            Map<String, Parameter> parameters,
            EnumSet<Usage> usage,
            Function<Map<String, String>, ?> action) {
        super(methodName, description, parameters, usage);
        this.action = action;
    }

    public Function<Map<String, String>, ?> getAction() {
        return action;
    }

    public void setAction(Function<Map<String, String>, ?> action) {
        this.action = action;
    }
}
