package io.quarkus.gradle.application.internal.image;

public record ContainerImageTarget(String reference) {

    public ContainerImageTarget {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Container image reference must not be empty");
        }
    }
}
