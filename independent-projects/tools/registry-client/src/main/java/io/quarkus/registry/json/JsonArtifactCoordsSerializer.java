package io.quarkus.registry.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.quarkus.maven.ArtifactCoords;
import java.io.IOException;

public class JsonArtifactCoordsSerializer extends JsonSerializer<ArtifactCoords> {
    @Override
    public void serialize(ArtifactCoords value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
