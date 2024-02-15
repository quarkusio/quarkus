package io.quarkus.annotation.processor;

/**
 * Define the output here so that they are clearly identified.
 * <p>
 * Paths are resolved from target/classes.
 */
public final class Outputs {

    public static final String META_INF_QUARKUS_BUILD_STEPS = "META-INF/quarkus-build-steps.list";
    public static final String META_INF_QUARKUS_CONFIG_ROOTS = "META-INF/quarkus-config-roots.list";
    public static final String META_INF_QUARKUS_JAVADOC = "META-INF/quarkus-javadoc.properties";

    /**
     * This directory is specific and written directly into target/ as it's not a file we want to package.
     */
    public static final String QUARKUS_CONFIG_DOC = "quarkus-config-doc";

    private Outputs() {
    }
}
