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
public class JsonExtension implements Extension {

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

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ArtifactCoords getArtifact() {
        if (artifact == null && artifactId != null) {
            artifact = new ArtifactCoords(groupId, artifactId, version);
        }
        return artifact;
    }

    public void setArtifact(ArtifactCoords coords) {
        this.artifact = coords;
    }

    public void setOrigins(List<ExtensionOrigin> origins) {
        this.origins = origins;
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

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Extension addMetadata(String key, Object value) {
        this.getMetadata().put(key, value);
        return this;

    }

    public Extension removeMetadata(String key) {
        this.getMetadata().remove(key);
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
}
