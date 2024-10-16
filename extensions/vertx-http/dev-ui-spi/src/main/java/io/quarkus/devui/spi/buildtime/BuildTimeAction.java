package io.quarkus.devui.spi.buildtime;

import java.util.Map;
import java.util.function.Function;

import io.quarkus.runtime.RuntimeValue;

/**
 * Define a action that can be executed against the deployment classpath in runtime
 * This means a call will still be make with Json-RPC to the backend, but fall through to this action
 */
public class BuildTimeAction {

    private final String methodName;
    private final Function<Map<String, String>, ?> action;
    private final RuntimeValue runtimeValue;

    protected <T> BuildTimeAction(String methodName,
            Function<Map<String, String>, T> action) {

        this.methodName = methodName;
        this.action = action;
        this.runtimeValue = null;
    }

    protected <T> BuildTimeAction(String methodName,
            RuntimeValue runtimeValue) {

        this.methodName = methodName;
        this.action = null;
        this.runtimeValue = runtimeValue;
    }

    public String getMethodName() {
        return methodName;
    }

    public Function<Map<String, String>, ?> getAction() {
        return action;
    }

    public RuntimeValue getRuntimeValue() {
        return runtimeValue;
    }

    public boolean hasRuntimeValue() {
        return this.runtimeValue != null;
    }
}
