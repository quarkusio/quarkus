package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonPlatformCatalog extends JsonEntityWithAnySupport implements PlatformCatalog.Mutable {

    private Map<String, Platform> platforms;

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

    public Mutable setPlatforms(Collection<Platform> newValues) {
        for (Platform p : newValues) {
            addPlatform(p);
        }
        return this;
    }

    public Mutable addPlatform(Platform platform) {
        if (platforms == null) {
            platforms = new LinkedHashMap<>();
        }
        platforms.put(platform.getPlatformKey(), platform);
        return this;
    }

    @Override
    public JsonPlatformCatalog setMetadata(Map<String, Object> metadata) {
        super.setMetadata(metadata);
        return this;
    }

    @Override
    public JsonPlatformCatalog setMetadata(String name, Object value) {
        super.setMetadata(name, value);
        return this;
    }

    @Override
    public JsonPlatformCatalog removeMetadata(String key) {
        super.removeMetadata(key);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonPlatformCatalog that = (JsonPlatformCatalog) o;
        return Objects.equals(platforms, that.platforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platforms);
    }

    @Override
    public Mutable mutable() {
        return this;
    }

    @Override
    public JsonPlatformCatalog build() {
        return this;
    }
}
