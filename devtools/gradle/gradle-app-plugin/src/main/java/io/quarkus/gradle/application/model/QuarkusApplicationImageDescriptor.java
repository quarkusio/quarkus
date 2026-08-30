package io.quarkus.gradle.application.model;

import java.util.Optional;

/**
 * Immutable effective container-image coordinates and optional builder.
 *
 * @param repository the non-blank image repository, without the tag
 * @param tag the non-blank image tag
 * @param builder the selected builder, or {@code null} when Quarkus/provider
 *        defaults choose it
 */
public record QuarkusApplicationImageDescriptor(String repository, String tag, QuarkusApplicationImageBuilder builder) {

    /**
     * Creates and validates an image descriptor.
     */
    public QuarkusApplicationImageDescriptor {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Image repository must not be empty");
        }
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Image tag must not be empty");
        }
    }

    /**
     * Returns the repository and tag in container-image reference form.
     *
     * @return {@code repository:tag}
     */
    public String effectiveReference() {
        return repository + ":" + tag;
    }

    /**
     * Returns the explicitly selected builder.
     *
     * @return the builder, or empty when no builder was selected
     */
    public Optional<QuarkusApplicationImageBuilder> optionalBuilder() {
        return Optional.ofNullable(builder);
    }
}
