package io.quarkus.rest.data.panache.deployment.methods;

public final class MethodMetadata {

    private final String name;

    private final String[] parameterTypes;

    public MethodMetadata(String name, String... parameterTypes) {
        this.name = name;
        this.parameterTypes = parameterTypes;
    }

    public String getName() {
        return name;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }
}
