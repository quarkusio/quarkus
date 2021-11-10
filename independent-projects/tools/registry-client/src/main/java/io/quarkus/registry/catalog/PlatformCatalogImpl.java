package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.registry.catalog.json.JsonPlatform;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = PlatformCatalogImpl.Builder.class)
public class PlatformCatalogImpl extends CatalogMetadata implements PlatformCatalog {

    private final Map<String, Platform> platforms;

    public PlatformCatalogImpl(Builder builder) {
        super(builder);
        this.platforms = builder.platforms == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.platforms);
    }

    @Override
    @JsonDeserialize(contentAs = PlatformImpl.class)
    public Collection<Platform> getPlatforms() {
        return platforms.values();
    }

    @Override
    @JsonIgnore
    public Platform getPlatform(String platformId) {
        return platforms == null ? null : platforms.get(platformId);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends CatalogMetadata.Builder implements PlatformCatalog {
        private Map<String, Platform> platforms;

        public Builder() {
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            super.withMetadata(metadata);
            return this;
        }

        public Builder withPlatforms(List<Platform> platforms) {
            for (Platform p : platforms) {
                addPlatform(p);
            }
            return this;
        }

        public Builder addPlatform(Platform platform) {
            if (platforms == null) {
                platforms = new LinkedHashMap<>();
            }
            platforms.put(platform.getPlatformKey(), platform);
            return this;
        }

        @Override
        @JsonDeserialize(contentAs = JsonPlatform.class)
        public Collection<Platform> getPlatforms() {
            return platforms == null ? Collections.emptyList() : platforms.values();
        }

        @Override
        @JsonIgnore
        public Platform getPlatform(String platformId) {
            return platforms == null ? null : platforms.get(platformId);
        }

        public PlatformCatalogImpl build() {
            return new PlatformCatalogImpl(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof PlatformCatalog)) {
            return false;
        }
        PlatformCatalog that = (PlatformCatalog) o;
        return Objects.equals(this.getPlatforms(), that.getPlatforms());
    }

    @Override
    public int hashCode() {
        return Objects.hash(platforms);
    }
}
