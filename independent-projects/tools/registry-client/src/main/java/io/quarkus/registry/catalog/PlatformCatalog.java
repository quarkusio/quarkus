package io.quarkus.registry.catalog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.registry.json.JsonBuilder;

public interface PlatformCatalog {

    Collection<Platform> getPlatforms();

    Map<String, Object> getMetadata();

    Platform getPlatform(String platformId);

    @JsonIgnore
    default Platform getRecommendedPlatform() {
        final Collection<Platform> platforms = getPlatforms();
        return platforms.isEmpty() ? null : platforms.iterator().next();
    }

    /**
     * @return a mutable copy of this configuration
     */
    default Mutable mutable() {
        return new PlatformCatalogImpl.Builder(this);
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

    interface Mutable extends PlatformCatalog, JsonBuilder<PlatformCatalog> {

        Mutable addPlatform(Platform platform);

        Mutable setPlatforms(Collection<Platform> newValues);

        Mutable setMetadata(Map<String, Object> metadata);

        Mutable setMetadata(String key, Object value);

        Mutable removeMetadata(String key);

        PlatformCatalog build();

        default void persist(Path p) throws IOException {
            CatalogMapperHelper.serialize(this.build(), p);
        }
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new PlatformCatalogImpl.Builder();
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return read-only PlatformCatalog object
     */
    static PlatformCatalog fromFile(Path path) throws IOException {
        return mutableFromFile(path).build();
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return read-only PlatformCatalog object (empty/default for an empty file)
     */
    static PlatformCatalog.Mutable mutableFromFile(Path path) throws IOException {
        PlatformCatalog.Mutable mutable = CatalogMapperHelper.deserialize(path, PlatformCatalogImpl.Builder.class);
        return mutable == null ? PlatformCatalog.builder() : mutable;
    }
}
