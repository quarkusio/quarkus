package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import io.quarkus.registry.catalog.PlatformStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonPlatformStream extends JsonEntityWithAnySupport implements PlatformStream {

    private String id;
    private String name;
    private Map<PlatformReleaseVersion, PlatformRelease> releases;

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
    public Collection<PlatformRelease> getReleases() {
        return releases == null ? Collections.emptyList() : releases.values();
    }

    @Override
    @JsonIgnore
    public PlatformRelease getRelease(PlatformReleaseVersion version) {
        return releases == null ? null : releases.get(version);
    }

    public void setReleases(List<PlatformRelease> releases) {
        if (this.releases != null) {
            this.releases.clear();
        }
        for (PlatformRelease r : releases) {
            addRelease(r);
        }
    }

    public void addRelease(PlatformRelease platformRelease) {
        if (releases == null) {
            releases = new LinkedHashMap<>();
        }
        releases.put(platformRelease.getVersion(), platformRelease);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonPlatformStream that = (JsonPlatformStream) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + releases;
    }

}
