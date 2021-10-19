package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.maven.ArtifactCoords;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = ExtensionImpl.Builder.class)
public class ExtensionImpl extends CatalogMetadata implements Extension {
    private final String name;
    private final String description;
    private final ArtifactCoords artifact;
    private final List<ExtensionOrigin> origins;

    private ExtensionImpl(Builder builder) {
        super(builder);
        this.artifact = builder.getArtifact();
        this.description = builder.getDescription();
        this.name = builder.getName();
        this.origins = builder.getOrigins();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    @Override
    public List<ExtensionOrigin> getOrigins() {
        return origins;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Extension builder(Extension e) {
        return new Builder()
                .withArtifact(e.getArtifact())
                .withName(e.getName())
                .withDescription(e.getDescription())
                .withMetadata(e.getMetadata())
                .withOrigins(new ArrayList<>(e.getOrigins()));
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends CatalogMetadata.Builder implements Extension {
        protected String name;
        protected String description;
        protected ArtifactCoords artifact;
        protected List<ExtensionOrigin> origins;

        protected String groupId;
        protected String artifactId;
        protected String version;

        public Builder() {
        }

        public Builder withGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder withArtifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            super.withMetadata(metadata);
            return this;
        }

        public Builder withVersion(String version) {
            this.name = version;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withArtifact(ArtifactCoords artifact) {
            this.artifact = artifact;
            return this;
        }

        public Builder withOrigins(List<ExtensionOrigin> origins) {
            this.origins = origins;
            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public ArtifactCoords getArtifact() {
            if (artifact == null && artifactId != null) {
                artifact = new ArtifactCoords(groupId, artifactId, version);
            }
            return artifact;
        }

        @Override
        @JsonIdentityReference(alwaysAsId = true)
        @JsonDeserialize(contentAs = ExtensionOriginImpl.class)
        public List<ExtensionOrigin> getOrigins() {
            return origins == null ? Collections.emptyList() : origins;
        }

        public ExtensionImpl build() {
            return new ExtensionImpl(this);
        }
    }

    @Override
    public String toString() {
        return name + " " + getArtifact();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Extension.class.isInstance(o)) {
            return false;
        }
        Extension extension = (Extension) o;
        return Objects.equals(artifact, extension.getArtifact());
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact);
    }
}
