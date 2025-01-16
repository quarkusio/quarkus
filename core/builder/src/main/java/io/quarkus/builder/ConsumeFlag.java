package io.quarkus.builder;

public enum ConsumeFlag {
    /**
     * Do not exclude the build step even if the given resource is not produced by any other build step.
     */
    OPTIONAL,
}
