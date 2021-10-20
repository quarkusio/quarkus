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
import java.util.Map;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonPlatformStream extends JsonEntityWithAnySupport implements PlatformStream.Mutable {

    private String id;
    private String name;
    private Map<PlatformReleaseVersion, PlatformRelease> releases;

    @Override
    public String getId() {
        return id;
    }

    public JsonPlatformStream setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public JsonPlatformStream setName(String name) {
        this.name = name;
        return this;
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

    public JsonPlatformStream setReleases(Collection<PlatformRelease> newValues) {
        if (this.releases != null) {
            this.releases.clear();
        }
        for (PlatformRelease r : newValues) {
            addRelease(r);
        }
        return this;
    }

    public Mutable addRelease(PlatformRelease platformRelease) {
        if (releases == null) {
            releases = new LinkedHashMap<>();
        }
        releases.put(platformRelease.getVersion(), platformRelease);
        return this;
    }

    @Override
    public JsonPlatformStream setMetadata(Map<String, Object> metadata) {
        super.setMetadata(metadata);
        return this;
    }

    @Override
    public JsonPlatformStream setMetadata(String name, Object value) {
        super.setMetadata(name, value);
        return this;
    }

    @Override
    public JsonPlatformStream removeMetadata(String key) {
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

    @Override
    public Mutable mutable() {
        return this;
    }

    @Override
    public JsonPlatformStream build() {
        return this;
    }
}
