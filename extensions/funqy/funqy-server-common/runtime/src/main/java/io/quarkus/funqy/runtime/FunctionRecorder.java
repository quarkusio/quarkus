package io.quarkus.funqy.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FunctionRecorder {
    public static FunctionRegistry registry;

    public void init() {
        registry = new FunctionRegistry();
    }

    public void register(Class functionClass, String methodName) {
        registry.register(functionClass, methodName, methodName);
    }

    public void register(Class functionClass, String methodName, String functionName) {
        registry.register(functionClass, methodName, functionName);
    }
}
