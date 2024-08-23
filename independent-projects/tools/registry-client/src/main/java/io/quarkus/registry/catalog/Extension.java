package io.quarkus.registry.catalog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

public interface Extension {

    String MD_SHORT_NAME = "short-name";
    String MD_NESTED_CODESTART_NAME = "codestart.name";
    String MD_NESTED_CODESTART_LANGUAGES = "codestart.languages";
    String MD_NESTED_CODESTART_KIND = "codestart.kind";
    String MD_NESTED_CODESTART_ARTIFACT = "codestart.artifact";

    String MD_GUIDE = "guide";

    String MD_MINIMUM_JAVA_VERSION = "minimum-java-version";
    String MD_KEYWORDS = "keywords";
    String MD_UNLISTED = "unlisted";
    String MD_CATEGORIES = "categories";
    String MD_STATUS = "status";
    String MD_BUILT_WITH_QUARKUS_CORE = "built-with-quarkus-core";
    String MD_CLI_PLUGINS = "cli-plugins";

    String getName();

    String getDescription();

    ArtifactCoords getArtifact();

    List<ExtensionOrigin> getOrigins();

    default boolean hasPlatformOrigin() {
        final List<ExtensionOrigin> origins = getOrigins();
        if (origins == null || origins.isEmpty()) {
            return false;
        }
        for (ExtensionOrigin o : origins) {
            if (o.isPlatform()) {
                return true;
            }
        }
        return false;
    }

    Map<String, Object> getMetadata();

    default String managementKey() {
        final ArtifactCoords artifact = getArtifact();
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    default Mutable mutable() {
        return new ExtensionImpl.Builder(this);
    }

    /**
     * Persist this configuration to the specified file.
     *
     * @param p Target path
     * @throws IOException if the specified file can not be written to.
     */
    default void persist(Path p) throws IOException {
        CatalogMapperHelper.serialize(this, p);
    }

    interface Mutable extends Extension, JsonBuilder<Extension> {

        Mutable setGroupId(String groupId);

        Mutable setArtifactId(String artifactId);

        Mutable setVersion(String version);

        Mutable setName(String name);

        Mutable setDescription(String description);

        Mutable setMetadata(Map<String, Object> metadata);

        Mutable setMetadata(String name, Object value);

        Mutable removeMetadata(String key);

        Mutable setArtifact(ArtifactCoords artifact);

        Mutable setOrigins(List<ExtensionOrigin> origins);

        @Override
        Extension build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new ExtensionImpl.Builder();
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return read-only Extension object
     */
    static Extension fromFile(Path path) throws IOException {
        return mutableFromFile(path).build();
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return read-only Extension object (empty/default for an empty file)
     */
    static Extension.Mutable mutableFromFile(Path path) throws IOException {
        Extension.Mutable mutable = CatalogMapperHelper.deserialize(path, ExtensionImpl.Builder.class);
        return mutable == null ? Extension.builder() : mutable;
    }
}
