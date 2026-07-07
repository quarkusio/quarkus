package io.quarkus.gradle.application.dsl;

import org.gradle.api.provider.Property;

/**
 * Configures an image that extends the named output's ordinary,
 * archive-free image with its typed startup archive.
 * <p>
 * This block is selected only through {@link QuarkusAotJarOutput#startupOptimizedImage(org.gradle.api.Action)}, which
 * registers build and push tasks. The selected archive must have a concrete type, a valid file or directory source, and
 * a non-blank suffix that does not overwrite the base image reference.
 */
public abstract class QuarkusApplicationStartupOptimizedImage {

    /**
     * Returns the suffix appended to the resolved base image reference.
     * <p>
     * Its lazy convention comes from the selected startup archive type, for example {@code -aot} or {@code -scc}.
     *
     * @return the required non-blank image suffix
     */
    public abstract Property<String> getImageSuffix();
}
