package io.quarkus.registry.config.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.quarkus.registry.config.RegistryConfig;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonRegistryConfigDeserializer extends JsonDeserializer<Map<String, RegistryConfig>> {
    final static TypeReference<LinkedHashMap<String, JsonRegistryConfig>> mapRef = new TypeReference<>() {
    };

    @Override
    public Map<String, RegistryConfig> deserialize(JsonParser jp, DeserializationContext ctx)
            throws IOException, JsonProcessingException {
        final Map<String, RegistryConfig> result;
        if (jp.getCurrentToken() == JsonToken.START_ARRAY) {
            // read the string array format
            result = new LinkedHashMap<>();
            for (JsonToken token = jp.nextToken(); token != JsonToken.END_ARRAY; token = jp.nextToken()) {
                if (token == JsonToken.VALUE_STRING) {
                    String id = jp.getText();
                    result.put(id, null);
                } else if (token == JsonToken.START_OBJECT) {
                    ensureNextToken(jp, JsonToken.FIELD_NAME, ctx);
                    String id = jp.getCurrentName();
                    ensureNextToken(jp, JsonToken.START_OBJECT, ctx);
                    result.put(id, jp.readValueAs(JsonRegistryConfig.class));
                    ensureNextToken(jp, JsonToken.END_OBJECT, ctx);
                }
            }
        } else {
            // read the map format
            result = jp.readValueAs(mapRef);
        }
        result.replaceAll((k, v) -> {
            if (v == null) {
                return new JsonRegistryConfig().completeRequiredConfig(k);
            } else {
                JsonRegistryConfig cfg = JsonRegistryConfig.class.cast(v);
                return cfg.completeRequiredConfig(k);
            }
        });
        return result;
    }

    private void ensureNextToken(JsonParser p, JsonToken expected, DeserializationContext ctxt) throws IOException {
        if (p.nextToken() != expected) {
            throw InvalidFormatException.from(p, "Expected " + expected, ctxt, JsonRegistryConfig.class);
        }
    }
}
