package io.quarkus.bootstrap;

/**
 *
 * @author Alexey Loubyansky
 */
public interface BootstrapConstants {

    String SERIALIZED_APP_MODEL = "quarkus-internal.serialized-app-model.path";
    String SERIALIZED_TEST_APP_MODEL = "quarkus-internal-test.serialized-app-model.path";
    String DESCRIPTOR_FILE_NAME = "quarkus-extension.properties";
    String CONDITIONAL_DEPENDENCIES = "conditional-dependencies";
    String CONDITIONAL_DEV_DEPENDENCIES = "conditional-dev-dependencies";
    String DEPENDENCY_CONDITION = "dependency-condition";

    /**
     * Constant for sharing the additional mappings between test-sources and the corresponding application-sources.
     * The Gradle plugin populates this data which is then read by the PathTestHelper when executing tests.
     */
    String TEST_TO_MAIN_MAPPINGS = "TEST_TO_MAIN_MAPPINGS";

    String OUTPUT_SOURCES_DIR = "OUTPUT_SOURCES_DIR";

    String QUARKUS_EXTENSION_FILE_NAME = "quarkus-extension.yaml";

    String META_INF = "META-INF";

    String DESCRIPTOR_PATH = META_INF + '/' + DESCRIPTOR_FILE_NAME;
    String BUILD_STEPS_PATH = META_INF + "/quarkus-build-steps.list";
    String EXTENSION_METADATA_PATH = META_INF + '/' + QUARKUS_EXTENSION_FILE_NAME;

    String PROP_DEPLOYMENT_ARTIFACT = "deployment-artifact";
    String PROP_PROVIDES_CAPABILITIES = "provides-capabilities";
    String PROP_REQUIRES_CAPABILITIES = "requires-capabilities";
    String PARENT_FIRST_ARTIFACTS = "parent-first-artifacts";
    String EXCLUDED_ARTIFACTS = "excluded-artifacts";
    String LESSER_PRIORITY_ARTIFACTS = "lesser-priority-artifacts";

    String EMPTY = "";

    String PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX = "-quarkus-platform-descriptor";
    String PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX = "-quarkus-platform-properties";

    String PLATFORM_PROPERTY_PREFIX = "platform.";

    String QUARKUS_BOOTSTRAP_WORKSPACE_DISCOVERY = "quarkus.bootstrap.workspace-discovery";

    /**
     * Prefix for properties configuring extension Dev mode JVM arguments
     */
    String EXT_DEV_MODE_JVM_OPTION_PREFIX = "dev-mode.jvm-option.";

    /**
     * {@code quarkus-extension.properties} property listing JVM options whose values shouldn't change by
     * the default parameters values of the Quarkus Maven and Gradle plugins launching an application in dev mode
     */
    String EXT_DEV_MODE_LOCK_JVM_OPTIONS = "dev-mode.lock.jvm-options";

    /**
     * {@code quarkus-extension.properties} property listing JVM XX options whose values shouldn't change by
     * the default parameters values of the Quarkus Maven and Gradle plugins launching an application in dev mode
     */
    String EXT_DEV_MODE_LOCK_XX_JVM_OPTIONS = "dev-mode.lock.xx-jvm-options";
}
