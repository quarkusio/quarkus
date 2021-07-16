package io.quarkus.registry.config.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.quarkus.registry.Constants;
import io.quarkus.registry.config.RegistryConfig;
import java.io.IOException;
import java.util.Map;

public class JsonRegistryConfigSerializer extends JsonSerializer<Map<String, RegistryConfig>> {

    @Override
    public void serialize(Map<String, RegistryConfig> registryConfigMap, JsonGenerator gen,
            SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        for (Map.Entry<String, RegistryConfig> entry : registryConfigMap.entrySet()) {
            JsonRegistryConfig cfg = JsonRegistryConfig.class.cast(entry.getValue());
            if (cfg.isIdOnly() || Constants.DEFAULT_REGISTRY_ID.equals(cfg.getId())) {
                gen.writeObjectFieldStart(entry.getKey());
                gen.writeEndObject();
            } else {
                gen.writeFieldName(entry.getKey());
                serializerProvider.defaultSerializeValue(cfg, gen);
            }
        }
        gen.writeEndObject();
    }
}
