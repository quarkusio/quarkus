package io.quarkus.registry.config.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonRegistryConfigSerializer extends JsonSerializer<JsonRegistryConfig> {

    private JsonSerializer<Object> qerSerializer;

    @Override
    public void serialize(JsonRegistryConfig value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value.isIdOnly()) {
            gen.writeString(value.getId());
        } else {
            gen.writeStartObject();
            gen.writeObjectFieldStart(value.getId());

            // for some reason the any serialization is not going to work, so here is a workaround
            Map<String, Object> extra = value.getExtra();
            if (!extra.isEmpty()) {
                extra = new HashMap<>(extra);
                value.getExtra().clear();
            }
            getQerSerializer(serializers).unwrappingSerializer(null).serialize(value, gen, serializers);

            if (!extra.isEmpty()) {
                for (Map.Entry<?, ?> entry : extra.entrySet()) {
                    gen.writeObjectField(entry.getKey().toString(), entry.getValue());
                }
            }

            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    private JsonSerializer<Object> getQerSerializer(SerializerProvider serializers) throws JsonMappingException {
        if (qerSerializer == null) {
            final JavaType javaType = serializers.constructType(JsonRegistryConfig.class);
            final BeanDescription beanDesc = serializers.getConfig().introspect(javaType);
            qerSerializer = BeanSerializerFactory.instance.findBeanOrAddOnSerializer(serializers, javaType, beanDesc,
                    serializers.isEnabled(MapperFeature.USE_STATIC_TYPING));
        }
        return qerSerializer;
    }
}
