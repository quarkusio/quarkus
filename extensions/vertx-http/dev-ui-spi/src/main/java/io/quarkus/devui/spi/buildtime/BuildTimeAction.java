package io.quarkus.devui.spi.buildtime;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.quarkus.runtime.RuntimeValue;

/**
 * Define a action that can be executed against the deployment classpath in runtime
 * This means a call will still be make with Json-RPC to the backend, but fall through to this action
 */
public class BuildTimeAction {

    private final String methodName;
    private final Function<Map<String, String>, ?> action;
    private final BiFunction<Object, Map<String, String>, ?> assistantAction;
    private final RuntimeValue runtimeValue;

    protected <T> BuildTimeAction(String methodName,
            Function<Map<String, String>, T> action) {

        this.methodName = methodName;
        this.action = action;
        this.assistantAction = null;
        this.runtimeValue = null;
    }

    protected <T> BuildTimeAction(String methodName,
            BiFunction<Object, Map<String, String>, T> assistantAction) {

        this.methodName = methodName;
        this.action = null;
        this.assistantAction = assistantAction;
        this.runtimeValue = null;
    }

    protected <T> BuildTimeAction(String methodName,
            RuntimeValue runtimeValue) {

        this.methodName = methodName;
        this.action = null;
        this.assistantAction = null;
        this.runtimeValue = runtimeValue;
    }

    public String getMethodName() {
        return methodName;
    }

    public Function<Map<String, String>, ?> getAction() {
        return action;
    }

    public boolean hasAction() {
        return this.action != null;
    }

    public BiFunction<Object, Map<String, String>, ?> getAssistantAction() {
        return assistantAction;
    }

    public boolean hasAssistantAction() {
        return this.assistantAction != null;
    }

    public RuntimeValue getRuntimeValue() {
        return runtimeValue;
    }

    public boolean hasRuntimeValue() {
        return this.runtimeValue != null;
    }
}
