package io.quarkus.registry.json;

import io.quarkus.maven.dependency.ArtifactCoords;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
public class JsonArtifactCoordsSerializer extends ValueSerializer<ArtifactCoords> {
    @Override
    public void serialize(ArtifactCoords value, JsonGenerator gen, SerializationContext serializers) {
        gen.writeString(value.toString());
    }
}
