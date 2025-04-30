package io.quarkus.registry.catalog;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.registry.json.JsonBuilder;
import io.quarkus.registry.json.JsonEntityWithAnySupport;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see PlatformCatalog#builder() creates a builder
 * @see PlatformCatalog#mutable() creates a builder from an existing PlatformCatalog
 * @see PlatformCatalog#mutableFromFile(Path) creates a builder from the contents of a file
 * @see PlatformCatalog#fromFile(Path) creates (and builds) a builder from the contents of a file
 * @see PlatformCatalog#persist(Path) for writing an PlatformCatalog to a file
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "platforms", "metadata" })
public class PlatformCatalogImpl extends JsonEntityWithAnySupport implements PlatformCatalog {

    private final Map<String, Platform> platforms;

    public PlatformCatalogImpl(Builder builder) {
        super(builder);
        this.platforms = JsonBuilder.buildUnmodifiableMap(builder.platforms, LinkedHashMap::new);
    }

    @Override
    @JsonSerialize(contentAs = PlatformImpl.class)
    public Collection<Platform> getPlatforms() {
        return platforms.values();
    }

    @Override
    @JsonIgnore
    public Platform getPlatform(String platformId) {
        return platforms.get(platformId);
    }

    @Override
    public boolean equals(Object o) {
        return platformCatalogEquals(this, o, platforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platforms);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() +
                "{platforms=" + platforms +
                '}';
    }

    /**
     * Builder.
     */
    public static class Builder extends JsonEntityWithAnySupport.Builder implements PlatformCatalog.Mutable {
        private Map<String, Platform> platforms;

        public Builder() {
        }

        Builder(PlatformCatalog source) {
            setMetadata(source.getMetadata());
            setPlatforms(source.getPlatforms());
        }

        public Builder addPlatform(Platform platform) {
            if (platforms == null) {
                platforms = new LinkedHashMap<>();
            }
            platforms.put(platform.getPlatformKey(), platform);
            return this;
        }

        @Override
        public Collection<Platform> getPlatforms() {
            return platforms == null ? Collections.emptyList() : platforms.values();
        }

        @JsonDeserialize(contentAs = PlatformImpl.Builder.class)
        public Builder setPlatforms(Collection<Platform> newValues) {
            for (Platform p : newValues) {
                addPlatform(p);
            }
            return this;
        }

        @Override
        @JsonIgnore
        public Platform getPlatform(String platformId) {
            return platforms == null ? null : platforms.get(platformId);
        }

        @Override
        public Builder setMetadata(Map<String, Object> metadata) {
            super.setMetadata(metadata);
            return this;
        }

        @Override
        public Builder setMetadata(String name, Object value) {
            super.setMetadata(name, value);
            return this;
        }

        @Override
        public Builder removeMetadata(String key) {
            super.removeMetadata(key);
            return this;
        }

        @Override
        public PlatformCatalogImpl build() {
            return new PlatformCatalogImpl(this);
        }

        @Override
        public boolean equals(Object o) {
            return platformCatalogEquals(this, o, platforms);
        }

        @Override
        public int hashCode() {
            return Objects.hash(platforms);
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() +
                    "{platforms=" + platforms +
                    '}';
        }
    }

    static final boolean platformCatalogEquals(PlatformCatalog c, Object o,
            Map<String, Platform> myPlatforms) {
        if (c == o) {
            return true;
        }
        if (o instanceof PlatformCatalogImpl.Builder) {
            return Objects.equals(myPlatforms, ((PlatformCatalogImpl.Builder) o).platforms);
        }
        if (o instanceof PlatformCatalogImpl) {
            return Objects.equals(myPlatforms, ((PlatformCatalogImpl) o).platforms);
        }
        if (!(o instanceof PlatformCatalog)) {
            return false;
        }
        PlatformCatalog that = (PlatformCatalog) o;
        return Objects.equals(c.getPlatforms(), that.getPlatforms());
    }
}
