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

    // ApplicationModel Mappable keys
    String MAPPABLE_APP_ARTIFACT = "app-artifact";
    String MAPPABLE_PLATFORM_IMPORTS = "platform-imports";
    String MAPPABLE_CAPABILITIES = "capabilities";
    String MAPPABLE_LOCAL_PROJECTS = "local-projects";
    String MAPPABLE_EXCLUDED_RESOURCES = "excluded-resources";
    String MAPPABLE_EXTENSION_DEV_CONFIG = "extension-dev-config";
    // ArtifactDependency Mappable keys
    String MAPPABLE_MAVEN_ARTIFACT = "maven-artifact";
    String MAPPABLE_SCOPE = "scope";
    String MAPPABLE_FLAGS = "flags";
    String MAPPABLE_EXCLUSIONS = "exclusions";
    // ResolvedArtifactDependency Mappable keys
    String MAPPABLE_DEPENDENCIES = "dependencies";
    String MAPPABLE_MODULE = "module";
    String MAPPABLE_RESOLVED_PATHS = "resolved-paths";
    // WorkspaceModule Mappable keys
    String MAPPABLE_MODULE_ID = "id";
    String MAPPABLE_MODULE_DIR = "module-dir";
    String MAPPABLE_BUILD_DIR = "build-dir";
    String MAPPABLE_BUILD_FILES = "build-files";
    String MAPPABLE_ARTIFACT_SOURCES = "artifact-sources";
    String MAPPABLE_PARENT = "parent";
    String MAPPABLE_TEST_CP_DEPENDENCY_EXCLUSIONS = "test-cp-exclusions";
    String MAPPABLE_TEST_ADDITIONAL_CP_ELEMENTS = "test-additional-cp-elements";
    String MAPPABLE_DIRECT_DEP_CONSTRAINTS = "direct-dep-constraints";
    String MAPPABLE_DIRECT_DEPS = "direct-deps";
    // ArtifactSource Mappable keys
    String MAPPABLE_CLASSIFIER = "classifier";
    String MAPPABLE_SOURCES = "sources";
    String MAPPABLE_RESOURCES = "resources";
    // SourceDir Mappable keys
    String MAPPABLE_SRC_DIR = "dir";
    String MAPPABLE_SRC_PATH_FILTER = "src-path-filter";
    String MAPPABLE_DEST_DIR = "dest-dir";
    String MAPPABLE_DEST_PATH_FILTER = "dest-path-filter";
    String MAPPABLE_APT_SOURCES_DIR = "apt-sources-dir";
    // PathFilter Mappable keys
    String MAPPABLE_INCLUDES = "includes";
    String MAPPABLE_EXCLUDES = "excludes";
    // PlatformImports Mappable keys
    String MAPPABLE_PLATFORM_PROPS = "platform-properties";
    String MAPPABLE_PLATFORM_RELEASE_INFO = "release-info";
    String MAPPABLE_IMPORTED_BOMS = "imported-boms";
    String MAPPABLE_MISALIGNED_REPORT = "misaligned-report";
    String MAPPABLE_ALIGNED = "aligned";
    // PlatformReleaseInfo Mappable keys
    String MAPPABLE_PLATFORM_KEY = "platform-key";
    String MAPPABLE_STREAM = "stream";
    String MAPPABLE_VERSION = "version";
    String MAPPABLE_BOMS = "boms";
    // ExtensionCapabilities Mappable keys
    String MAPPABLE_EXTENSION = "extension";
    String MAPPABLE_PROVIDED = "provided";
    String MAPPABLE_REQUIRED = "required";
    // ExtensionDevModeConfig Mappable keys
    String MAPPABLE_JVM_OPTIONS = "jvm-options";
    String MAPPABLE_NAME = "name";
    String MAPPABLE_JVM_OPTION_GROUP_PREFIX = "group";
    String MAPPABLE_VALUES = "values";
    String MAPPABLE_LOCK_JVM_OPTIONS = "lock-jvm-options";
}
