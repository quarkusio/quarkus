package io.quarkus.bootstrap.model.gradle;

import java.util.Locale;

/**
 * Indicates that a Gradle sidecar does not correspond to the application model or Tooling API target with which it was
 * paired.
 * <p>
 * This is a correctness failure. Correlation validation detects accidentally mixed results; it is not an authentication
 * or integrity boundary.
 */
public class GradleApplicationModelSidecarMismatchException extends IllegalArgumentException {

    private static final long serialVersionUID = 356306829480760234L;

    private final Dimension dimension;

    /**
     * Creates a mismatch for one correlation dimension.
     *
     * @param dimension dimension that failed validation; must not be {@code null}
     * @param expected expected value used in the diagnostic
     * @param actual actual value used in the diagnostic
     * @throws NullPointerException if {@code dimension} is {@code null}
     */
    public GradleApplicationModelSidecarMismatchException(Dimension dimension, Object expected, Object actual) {
        super("Gradle application-model sidecar " + dimension.name().toLowerCase(Locale.ROOT)
                + " mismatch: expected " + expected + " but was " + actual);
        this.dimension = dimension;
    }

    /** @return correlation dimension that failed validation; never {@code null} */
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * Independently validated dimensions of a paired result.
     */
    public enum Dimension {
        /** Sidecar schema version. */
        SCHEMA,
        /** Requested Quarkus launch mode. */
        MODE,
        /** Target Gradle build-tree path. */
        TARGET,
        /** Canonical application/dependency/path/workspace-edge projection. */
        GRAPH
    }
}
