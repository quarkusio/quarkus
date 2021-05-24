package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JsonPlatformCatalog extends JsonEntityWithAnySupport implements PlatformCatalog {

    private List<Platform> platforms;

    @Override
    @JsonDeserialize(contentAs = JsonPlatform.class)
    public List<Platform> getPlatforms() {
        return platforms == null ? Collections.emptyList() : platforms;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    public void addPlatform(Platform platform) {
        if (platforms == null) {
            platforms = new ArrayList<>();
        }
        platforms.add(platform);
    }
}
