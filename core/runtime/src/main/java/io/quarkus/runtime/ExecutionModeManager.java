package io.quarkus.runtime;

public final class ExecutionModeManager {

    private static volatile ExecutionMode executionMode = ExecutionMode.UNSET;

    public static void staticInit() {
        executionMode = ExecutionMode.STATIC_INIT;
    }

    public static void runtimeInit() {
        executionMode = ExecutionMode.RUNTIME_INIT;
    }

    public static void running() {
        executionMode = ExecutionMode.RUNNING;
    }

    public static void unset() {
        executionMode = ExecutionMode.UNSET;
    }

    public static ExecutionMode getExecutionMode() {
        return executionMode;
    }
}
