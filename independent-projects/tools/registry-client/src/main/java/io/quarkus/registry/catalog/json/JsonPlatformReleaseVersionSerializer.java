package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class JsonPlatformReleaseVersionSerializer extends JsonSerializer<JsonPlatformReleaseVersion> {

    @Override
    public void serialize(JsonPlatformReleaseVersion value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(value.toString());
    }
}
