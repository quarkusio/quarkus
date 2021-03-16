package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonPlatformCatalog implements PlatformCatalog {

    private List<Platform> platforms;
    private ArtifactCoords defaultPlatform;

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

    @Override
    public ArtifactCoords getDefaultPlatform() {
        return defaultPlatform;
    }

    public void setDefaultPlatform(ArtifactCoords defaultPlatform) {
        this.defaultPlatform = defaultPlatform;
    }
}
