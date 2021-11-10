package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.registry.catalog.json.JsonPlatformRelease;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = PlatformStreamImpl.Builder.class)
public class PlatformStreamImpl extends CatalogMetadata implements PlatformStream {
    private final String id;
    private final String name;
    private final Map<PlatformReleaseVersion, PlatformRelease> releases;

    private PlatformStreamImpl(Builder builder) {
        super(builder);
        this.id = builder.id;
        this.name = builder.name;
        this.releases = builder.releases == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.releases);

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @JsonDeserialize(contentAs = PlatformReleaseImpl.class)
    public Collection<PlatformRelease> getReleases() {
        return releases == null ? Collections.emptyList() : releases.values();
    }

    @Override
    @JsonIgnore
    public PlatformRelease getRelease(PlatformReleaseVersion version) {
        return releases == null ? null : releases.get(version);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends CatalogMetadata.Builder implements PlatformStream {
        private String id;
        private String name;
        private Map<PlatformReleaseVersion, PlatformRelease> releases;

        public Builder() {
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            super.withMetadata(metadata);
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withReleases(List<PlatformRelease> releases) {
            if (this.releases != null) {
                this.releases.clear();
            }
            for (PlatformRelease r : releases) {
                addRelease(r);
            }
            return this;
        }

        public Builder addRelease(PlatformRelease platformRelease) {
            if (releases == null) {
                releases = new LinkedHashMap<>();
            }
            releases.put(platformRelease.getVersion(), platformRelease);
            return this;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
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

        public PlatformStreamImpl build() {
            return new PlatformStreamImpl(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof PlatformStream)) {
            return false;
        }
        PlatformStream that = (PlatformStream) o;
        return Objects.equals(id, that.getId());
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
