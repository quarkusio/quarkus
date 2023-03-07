package io.quarkus.resteasy.reactive.server.runtime.devui;

public class ResteasyReactiveParamConverterProvider {
    private final String className;
    private final int priority;

    public ResteasyReactiveParamConverterProvider(String className, int priority) {
        this.className = className;
        this.priority = priority;
    }

    public String getClassName() {
        return className;
    }

    public int getPriority() {
        return priority;
    }
}
