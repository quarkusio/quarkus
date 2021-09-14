package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class JsonPlatformReleaseVersionDeserializer extends JsonDeserializer<JsonPlatformReleaseVersion> {

    @Override
    public JsonPlatformReleaseVersion deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        return JsonPlatformReleaseVersion.fromString(p.getText());
    }

}
