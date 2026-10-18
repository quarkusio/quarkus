package io.quarkus.registry.json;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.maven.dependency.ArtifactKey;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
@JsonSerialize(using = JsonArtifactCoordsSerializer.class)
@JsonDeserialize(using = JsonArtifactCoordsDeserializer.class)
public interface JsonArtifactCoordsMixin {

    @JsonIgnore
    ArtifactKey getKey();
}
