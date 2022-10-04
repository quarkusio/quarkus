package io.quarkus.registry.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
public class JsonArtifactCoordsSerializer extends JsonSerializer<ArtifactCoords> {
    @Override
    public void serialize(ArtifactCoords value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
