package io.quarkus.registry.json;

import io.quarkus.maven.dependency.ArtifactCoords;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
public class JsonArtifactCoordsDeserializer extends ValueDeserializer<ArtifactCoords> {

    @Override
    public ArtifactCoords deserialize(JsonParser p, DeserializationContext ctxt) {
        return ArtifactCoords.fromString(p.getString());
    }
}
