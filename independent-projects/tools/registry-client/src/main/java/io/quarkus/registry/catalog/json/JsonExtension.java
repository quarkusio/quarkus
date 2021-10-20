package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonExtension implements Extension.Mutable {

    public static JsonExtension copy(Extension e) {
        final JsonExtension copy = new JsonExtension();
        copy.setArtifact(e.getArtifact());
        copy.setName(e.getName());
        copy.setDescription(e.getDescription());
        if (!e.getMetadata().isEmpty()) {
            copy.setMetadata(new HashMap<>(e.getMetadata()));
        }
        copy.setOrigins(new ArrayList<>(e.getOrigins()));
        return copy;
    }

    private String name;
    private String description;
    private Map<String, Object> metadata;
    private ArtifactCoords artifact;
    private List<ExtensionOrigin> origins;

    // to support legacy format
    private String groupId;
    private String artifactId;
    private String version;

    public Extension.Mutable setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public Extension.Mutable setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public Extension.Mutable setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public Extension.Mutable setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public Extension.Mutable setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public ArtifactCoords getArtifact() {
        if (artifact == null && artifactId != null) {
            artifact = new ArtifactCoords(groupId, artifactId, version);
        }
        return artifact;
    }

    public Extension.Mutable setArtifact(ArtifactCoords coords) {
        this.artifact = coords;
        return this;
    }

    public Extension.Mutable setOrigins(List<ExtensionOrigin> origins) {
        this.origins = origins;
        return this;
    }

    @Override
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(contentAs = JsonExtensionOrigin.class)
    public List<ExtensionOrigin> getOrigins() {
        return origins == null ? Collections.emptyList() : origins;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata == null ? metadata = new HashMap<>() : metadata;
    }

    public Extension.Mutable setMetadata(Map<String, Object> newValues) {
        if (newValues != Collections.EMPTY_MAP) { // don't keep the empty map
            metadata = newValues;
        }
        return this;
    }

    public Extension.Mutable setMetadata(String key, Object value) {
        getMetadata().put(key, value);
        return this;
    }

    public Extension.Mutable removeMetadata(String key) {
        getMetadata().remove(key);
        return this;
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonExtension extension = (JsonExtension) o;
        return Objects.equals(artifact, extension.artifact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact);
    }

    @Override
    public Extension build() {
        return this;
    }

    @Override
    public Extension.Mutable mutable() {
        return copy(this);
    }
}
