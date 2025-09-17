package io.quarkus.devui.spi.buildtime.jsonrpc;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.quarkus.runtime.annotations.Usage;

/**
 * Deployment json-rpc methods. Here we need to know the function to call
 */
public final class DeploymentJsonRpcMethod extends AbstractJsonRpcMethod {

    private Function<Map<String, String>, ?> action;
    private BiFunction<Object, Map<String, String>, ?> assistantAction;

    public DeploymentJsonRpcMethod() {
    }

    public DeploymentJsonRpcMethod(String methodName,
            String description,
            EnumSet<Usage> usage,
            boolean mcpEnabledByDefault,
            Function<Map<String, String>, ?> action) {
        super(methodName, description, usage, mcpEnabledByDefault);
        this.action = action;
    }

    public DeploymentJsonRpcMethod(String methodName,
            String description,
            Map<String, Parameter> parameters,
            EnumSet<Usage> usage,
            boolean mcpEnabledByDefault,
            Function<Map<String, String>, ?> action) {
        super(methodName, description, parameters, usage, mcpEnabledByDefault);
        this.action = action;
    }

    public DeploymentJsonRpcMethod(String methodName,
            String description,
            EnumSet<Usage> usage,
            boolean mcpEnabledByDefault,
            BiFunction<Object, Map<String, String>, ?> assistantAction) {
        super(methodName, description, usage, mcpEnabledByDefault);
        this.assistantAction = assistantAction;
    }

    public DeploymentJsonRpcMethod(String methodName,
            String description,
            Map<String, Parameter> parameters,
            EnumSet<Usage> usage,
            boolean mcpEnabledByDefault,
            BiFunction<Object, Map<String, String>, ?> assistantAction) {
        super(methodName, description, parameters, usage, mcpEnabledByDefault);
        this.assistantAction = assistantAction;
    }

    public Function<Map<String, String>, ?> getAction() {
        return action;
    }

    public void setAction(Function<Map<String, String>, ?> action) {
        this.action = action;
    }

    public boolean hasAction() {
        return this.action != null;
    }

    public BiFunction<Object, Map<String, String>, ?> getAssistantAction() {
        return assistantAction;
    }

    public void setAssistantAction(BiFunction<Object, Map<String, String>, ?> assistantAction) {
        this.assistantAction = assistantAction;
    }

    public boolean hasAssistantAction() {
        return this.assistantAction != null;
    }
}
