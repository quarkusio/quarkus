package io.quarkus.bootstrap.model.gradle;

import java.util.Locale;

/**
 * Indicates that an IDE sidecar does not correspond to the application model
 * or Tooling API target with which it was paired.
 */
public class GradleApplicationModelSidecarMismatchException extends IllegalArgumentException {

    private static final long serialVersionUID = 356306829480760234L;

    private final Dimension dimension;

    public GradleApplicationModelSidecarMismatchException(Dimension dimension, Object expected, Object actual) {
        super("Gradle application-model sidecar " + dimension.name().toLowerCase(Locale.ROOT)
                + " mismatch: expected " + expected + " but was " + actual);
        this.dimension = dimension;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public enum Dimension {
        SCHEMA,
        MODE,
        TARGET,
        GRAPH
    }
}
