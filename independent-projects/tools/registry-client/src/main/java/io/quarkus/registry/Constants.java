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

    String IO_QUARKUS = "io.quarkus";
    String DEFAULT_PLATFORM_BOM_GROUP_ID = IO_QUARKUS + ".platform";
    String DEFAULT_PLATFORM_BOM_ARTIFACT_ID = "quarkus-bom";

    String JSON = "json";

    String LAST_UPDATED = "last-updated";
}
