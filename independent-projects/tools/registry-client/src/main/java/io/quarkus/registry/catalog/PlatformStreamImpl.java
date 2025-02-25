package io.quarkus.registry.catalog;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.registry.json.JsonBuilder;
import io.quarkus.registry.json.JsonEntityWithAnySupport;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see PlatformStream#mutable() creates a builder from an existing PlatformStream
 * @see PlatformStream#builder() creates a builder
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "id", "name", "releases", "metadata" })
public class PlatformStreamImpl extends JsonEntityWithAnySupport implements PlatformStream {
    private final String id;
    private final String name;
    private final Map<PlatformReleaseVersion, PlatformRelease> releases;

    private PlatformStreamImpl(Builder builder) {
        super(builder);
        this.id = builder.id;
        this.name = builder.name;
        this.releases = JsonBuilder.buildUnmodifiableMap(builder.releases, LinkedHashMap::new);
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
    public Collection<PlatformRelease> getReleases() {
        return releases.values();
    }

    @Override
    @JsonIgnore
    public PlatformRelease getRelease(PlatformReleaseVersion version) {
        return releases.get(version);
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
        return this.getClass().getSimpleName() +
                "{id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", releases=" + releases +
                '}';
    }

    /**
     * Builder.
     */
    public static class Builder extends JsonEntityWithAnySupport.Builder implements PlatformStream.Mutable {
        private String id;
        private String name;
        private Map<PlatformReleaseVersion, PlatformRelease> releases;

        public Builder() {
        }

        Builder(PlatformStream source) {
            this.id = source.getId();
            this.name = source.getName();
            setReleases(source.getReleases());
            setMetadata(source.getMetadata());
        }

        @Override
        public String getId() {
            return id;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Collection<PlatformRelease> getReleases() {
            return releases == null ? Collections.emptyList() : releases.values();
        }

        @JsonDeserialize(contentAs = PlatformReleaseImpl.Builder.class)
        public Builder setReleases(Collection<PlatformRelease> newReleases) {
            if (this.releases != null) {
                this.releases.clear();
            }
            for (PlatformRelease r : newReleases) {
                addRelease(r);
            }
            return this;
        }

        @Override
        @JsonIgnore
        public PlatformRelease getRelease(PlatformReleaseVersion version) {
            return releases == null ? null : releases.get(version);
        }

        @Override
        public Builder addRelease(PlatformRelease platformRelease) {
            if (releases == null) {
                releases = new LinkedHashMap<>();
            }
            releases.put(platformRelease.getVersion(), platformRelease);
            return this;
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
        public PlatformStreamImpl build() {
            return new PlatformStreamImpl(this);
        }
    }
}
