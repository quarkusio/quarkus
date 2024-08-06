package io.quarkus.annotation.processor;

import java.nio.file.Path;

/**
 * Define the output here so that they are clearly identified.
 * <p>
 * Paths are resolved from target/classes.
 */
public final class Outputs {

    public static final String META_INF_QUARKUS_BUILD_STEPS = "META-INF/quarkus-build-steps.list";
    public static final String META_INF_QUARKUS_CONFIG_ROOTS = "META-INF/quarkus-config-roots.list";

    public static final String META_INF_QUARKUS_CONFIG = "META-INF/quarkus-config";
    public static final String META_INF_QUARKUS_CONFIG_JAVADOC = META_INF_QUARKUS_CONFIG + "/quarkus-config-javadoc.json";
    public static final String META_INF_QUARKUS_CONFIG_MODEL = META_INF_QUARKUS_CONFIG + "/quarkus-config-model.json";

    /**
     * This directory is specific and written directly into target/ as it's not a file we want to package.
     * <p>
     * It is not written by the annotation processor Filer API so we can use proper Paths.
     */
    private static final Path QUARKUS_CONFIG_DOC = Path.of("quarkus-config-doc");
    public static final Path QUARKUS_CONFIG_DOC_JAVADOC = QUARKUS_CONFIG_DOC.resolve("quarkus-config-javadoc.yaml");
    public static final Path QUARKUS_CONFIG_DOC_MODEL = QUARKUS_CONFIG_DOC.resolve("quarkus-config-model.yaml");

    /**
     * Ideally, we should remove this file at some point.
     */
    @Deprecated(forRemoval = true)
    public static final String META_INF_QUARKUS_JAVADOC = "META-INF/quarkus-javadoc.properties";

    private Outputs() {
    }
}
