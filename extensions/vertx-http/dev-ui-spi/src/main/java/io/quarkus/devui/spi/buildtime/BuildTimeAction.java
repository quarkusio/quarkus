package io.quarkus.devui.spi.buildtime;

import java.util.Map;
import java.util.function.Function;

/**
 * Define a action that can be executed against the deployment classpath in runtime
 * This means a call will still be make with Json-RPC to the backend, but fall through to this action
 */
public class BuildTimeAction {

    private final String methodName;
    private final Function<Map<String, String>, ?> action;

    protected <T> BuildTimeAction(String methodName,
            Function<Map<String, String>, T> action) {

        this.methodName = methodName;
        this.action = action;

    }

    public String getMethodName() {
        return methodName;
    }

    public Function<Map<String, String>, ?> getAction() {
        return action;
    }
}
