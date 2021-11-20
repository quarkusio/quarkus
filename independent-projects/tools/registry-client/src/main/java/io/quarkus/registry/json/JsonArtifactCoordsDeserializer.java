package io.quarkus.registry.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.quarkus.maven.ArtifactCoords;
import java.io.IOException;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
public class JsonArtifactCoordsDeserializer extends JsonDeserializer<ArtifactCoords> {

    @Override
    public ArtifactCoords deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        return ArtifactCoords.fromString(p.getText());
    }
}
