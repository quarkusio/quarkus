package io.quarkus.funqy.runtime;

import java.util.List;

public class FunctionRecorder {
    public static volatile FunctionRegistry registry;

    public static FunctionRegistry init(List<Class<?>> functionClasses, List<String> methodNames, List<String> descriptors,
            List<String> functionNames) {
        FunctionRegistry newRegistry = new FunctionRegistry();
        for (int i = 0; i < functionClasses.size(); i++) {
            newRegistry.register(functionClasses.get(i), methodNames.get(i), descriptors.get(i), functionNames.get(i));
        }
        registry = newRegistry;
        return newRegistry;
    }
}
