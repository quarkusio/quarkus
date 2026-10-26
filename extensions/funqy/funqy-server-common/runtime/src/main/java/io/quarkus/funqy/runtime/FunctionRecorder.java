package io.quarkus.funqy.runtime;

import java.util.List;

/**
 * Creates the {@link FunctionRegistry} at static init and keeps it for the Funqy bindings.
 */
public class FunctionRecorder {
    public static volatile FunctionRegistry registry;

    /**
     * Create the registry and register every function in it.
     *
     * @param functions the functions found at build time
     * @return the new registry
     */
    public static FunctionRegistry init(List<FunctionDefinition> functions) {
        FunctionRegistry newRegistry = new FunctionRegistry();
        for (FunctionDefinition function : functions) {
            newRegistry.register(function.functionClass(), function.methodName(), function.descriptor(),
                    function.functionName());
        }
        registry = newRegistry;
        return newRegistry;
    }
}
