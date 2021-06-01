package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformStream;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JsonPlatformStream extends JsonEntityWithAnySupport implements PlatformStream {

    private String id;
    private String name;
    private List<PlatformRelease> releases;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonDeserialize(contentAs = JsonPlatformRelease.class)
    public List<PlatformRelease> getReleases() {
        return releases == null ? Collections.emptyList() : releases;
    }

    public void setReleases(List<PlatformRelease> releases) {
        this.releases = releases;
    }

    @Override
    public String toString() {
        return id + releases;
    }
}
