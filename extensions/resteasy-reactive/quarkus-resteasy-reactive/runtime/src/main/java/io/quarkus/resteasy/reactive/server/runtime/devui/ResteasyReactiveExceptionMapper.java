package io.quarkus.resteasy.reactive.server.runtime.devui;

public class ResteasyReactiveExceptionMapper {
    private final String name;
    private final String className;
    private final int priority;

    public ResteasyReactiveExceptionMapper(String name, String className, int priority) {
        this.name = name;
        this.className = className;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public int getPriority() {
        return priority;
    }
}
