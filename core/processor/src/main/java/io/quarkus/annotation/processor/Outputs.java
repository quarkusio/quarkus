package io.quarkus.annotation.processor;

/**
 * Define the outputs here so that they are clearly identified.
 * <p>
 * DO NOT use Path as we need maximum compatibility with the filer API and the ZipPath API.
 */
public final class Outputs {

    public static final String META_INF_QUARKUS_BUILD_STEPS = "META-INF/quarkus-build-steps.list";
    public static final String META_INF_QUARKUS_CONFIG_ROOTS = "META-INF/quarkus-config-roots.list";

    private static final String QUARKUS_CONFIG_DOC = "quarkus-config-doc";
    public static final String QUARKUS_CONFIG_DOC_JAVADOC = QUARKUS_CONFIG_DOC + "/quarkus-config-javadoc.yaml";
    public static final String QUARKUS_CONFIG_DOC_MODEL = QUARKUS_CONFIG_DOC + "/quarkus-config-model.yaml";

    public static final String META_INF_QUARKUS_CONFIG = "META-INF/" + QUARKUS_CONFIG_DOC;
    public static final String META_INF_QUARKUS_CONFIG_JAVADOC_JSON = META_INF_QUARKUS_CONFIG + "/quarkus-config-javadoc.json";
    public static final String META_INF_QUARKUS_CONFIG_MODEL_JSON = META_INF_QUARKUS_CONFIG + "/quarkus-config-model.json";
    public static final String META_INF_QUARKUS_CONFIG_MODEL_VERSION = META_INF_QUARKUS_CONFIG
            + "/quarkus-config-model-version";

    /**
     * Ideally, we should remove this file at some point.
     */
    @Deprecated(forRemoval = true)
    public static final String META_INF_QUARKUS_JAVADOC = "META-INF/quarkus-javadoc.properties";

    private Outputs() {
    }
}
