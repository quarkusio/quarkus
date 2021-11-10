package io.quarkus.registry.config.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;

@Deprecated
public class JsonRegistryConfigDeserializer extends JsonDeserializer<JsonRegistryConfig> {

    @Override
    public JsonRegistryConfig deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
            return new JsonRegistryConfig(p.getText());
        } else if (p.getCurrentToken() == JsonToken.START_OBJECT) {
            ensureNextToken(p, JsonToken.FIELD_NAME, ctxt);
            final String qerId = p.getCurrentName();
            ensureNextToken(p, JsonToken.START_OBJECT, ctxt);
            final JsonRegistryConfig qer = p.readValueAs(JsonRegistryConfig.class);
            qer.setId(qerId);
            ensureNextToken(p, JsonToken.END_OBJECT, ctxt);
            return qer;
        }
        return null;
    }

    private void ensureNextToken(JsonParser p, JsonToken expected, DeserializationContext ctxt) throws IOException {
        if (p.nextToken() != expected) {
            throw InvalidFormatException.from(p, "Expected " + expected, ctxt, JsonRegistryConfig.class);
        }
    }
}
