package io.quarkus.registry.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
@JsonSerialize(using = JsonArtifactCoordsSerializer.class)
@JsonDeserialize(using = JsonArtifactCoordsDeserializer.class)
public interface JsonArtifactCoordsMixin {

    @JsonIgnore
    ArtifactKey getKey();
}
