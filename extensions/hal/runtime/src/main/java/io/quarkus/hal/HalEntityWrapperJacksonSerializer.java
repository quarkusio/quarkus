package io.quarkus.hal;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

public class HalEntityWrapperJacksonSerializer extends JsonSerializer<HalEntityWrapper<?>> {

    @Override
    public void serialize(HalEntityWrapper wrapper, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        Object entity = wrapper.getEntity();

        generator.writeStartObject();
        for (BeanPropertyDefinition property : getPropertyDefinitions(serializers, entity.getClass())) {
            AnnotatedMember accessor = property.getAccessor();
            if (accessor != null) {
                Object value = accessor.getValue(entity);
                generator.writeFieldName(property.getName());
                if (value == null) {
                    generator.writeNull();
                } else {
                    serializers.findValueSerializer(value.getClass()).serialize(value, generator, serializers);
                }
            }
        }
        writeLinks(wrapper.getLinks(), generator);
        generator.writeEndObject();
    }

    private void writeLinks(Map<String, HalLink> links, JsonGenerator generator) throws IOException {
        generator.writeFieldName("_links");
        generator.writeObject(links);
    }

    private List<BeanPropertyDefinition> getPropertyDefinitions(SerializerProvider serializers, Class<?> entityClass) {
        JavaType entityType = serializers.getTypeFactory().constructType(entityClass);

        return new BasicClassIntrospector()
                .forSerialization(serializers.getConfig(), entityType, serializers.getConfig())
                .findProperties();
    }
}
