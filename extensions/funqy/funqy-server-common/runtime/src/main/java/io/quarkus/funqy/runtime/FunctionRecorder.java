package io.quarkus.funqy.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FunctionRecorder {
    public static FunctionRegistry registry;

    public void init() {
        registry = new FunctionRegistry();
    }

    public void register(Class functionClass, String methodName, String descriptor) {
        registry.register(functionClass, methodName, descriptor, methodName);
    }

    public void register(Class functionClass, String methodName, String descriptor, String functionName) {
        registry.register(functionClass, methodName, descriptor, functionName);
    }
}
