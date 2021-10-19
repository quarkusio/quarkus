package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonPlatformCatalog extends JsonEntityWithAnySupport implements PlatformCatalog {

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

    public void setPlatforms(List<Platform> platforms) {
        for (Platform p : platforms) {
            addPlatform(p);
        }
    }

    public void addPlatform(Platform platform) {
        if (platforms == null) {
            platforms = new LinkedHashMap<>();
        }
        platforms.put(platform.getPlatformKey(), platform);
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
}
