package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.quarkus.maven.ArtifactCoords;
import java.io.IOException;

@Deprecated
public class JsonArtifactCoordsDeserializer extends JsonDeserializer<ArtifactCoords> {

    @Override
    public ArtifactCoords deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        return ArtifactCoords.fromString(p.getText());
    }
}
