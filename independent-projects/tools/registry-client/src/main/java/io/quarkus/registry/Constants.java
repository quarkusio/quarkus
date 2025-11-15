package io.quarkus.registry;

public interface Constants {

    String DEFAULT_REGISTRY_ID = "registry.quarkus.io";
    String DEFAULT_REGISTRY_GROUP_ID = "io.quarkus.registry";
    String DEFAULT_REGISTRY_ARTIFACT_VERSION = "1.0-SNAPSHOT";
    String DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID = "quarkus-registry-descriptor";
    String DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID = "quarkus-non-platform-extensions";
    String DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID = "quarkus-platforms";

    String DEFAULT_REGISTRY_REST_URL = "https://registry.quarkus.io/api/1";
    String DEFAULT_REGISTRY_MAVEN_REPO_URL = "https://registry.quarkus.io/maven";

    String PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX = "-quarkus-platform-descriptor";
    String PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX = "-quarkus-platform-properties";

    String QUARKUS_VERSION_CLASSIFIER_ALL = "all";

    String JSON = "json";

    String LAST_UPDATED = "last-updated";

    /**
     * Registry client configuration option allowing users to limit the extension catalog from a registry to a specific offering
     */
    String OFFERING = "offering";

    /**
     * Registry client configuration option allowing users to set a low boundary for the recommended streams per platform
     */
    String RECOMMEND_STREAMS_FROM = "recommend-streams-from";

    /**
     * An internal key optionally added to extension metadata by a registry client
     * to indicate a user configured offering-based support key that should be displayed by extension listing commands
     */
    String REGISTRY_CLIENT_USER_SELECTED_SUPPORT_KEY = "registry-client:user-selected-support-key";

    /**
     * An internal key added to extension catalog metadata by a registry client
     * to provide information about the complete list of extension artifacts included in the catalog.
     * The value associated with this key will be a map of {@link io.quarkus.maven.dependency.ArtifactKey}
     * to {@link io.quarkus.maven.dependency.ArtifactCoords} of extension artifacts.
     * <p>
     * The list of extension artifacts will contain all the extension artifacts before the offering-based
     * filtered applied.
     */
    String REGISTRY_CLIENT_ALL_CATALOG_EXTENSIONS = "registry-client:offering-managed-not-supported-keys";

    /**
     * An internal key added to an extension catalog metadata by a registry client
     * to associate a given extension catalog with a preference code that will be used by an algorithm
     * selecting the latest recommended combination of extension and platform BOM versions.
     */
    String REGISTRY_CLIENT_ORIGIN_PREFERENCE = "registry-client:origin-preference";
}
