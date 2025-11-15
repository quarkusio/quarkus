package io.quarkus.devui.spi.buildtime.jsonrpc;

import java.util.EnumSet;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Usage;

/**
 * Recorded json-rpc methods. Here we need to know the recorded value
 */
public final class RecordedJsonRpcMethod extends AbstractJsonRpcMethod {

    private RuntimeValue runtimeValue;

    public RecordedJsonRpcMethod() {
    }

    public RecordedJsonRpcMethod(String methodName,
            String description,
            EnumSet<Usage> usage,
            boolean mcpEnabledByDefault,
            RuntimeValue runtimeValue) {
        super(methodName, description, usage, mcpEnabledByDefault);
        this.runtimeValue = runtimeValue;
    }

    public RuntimeValue getRuntimeValue() {
        return runtimeValue;
    }

    public void setRuntimeValue(RuntimeValue runtimeValue) {
        this.runtimeValue = runtimeValue;
    }
}