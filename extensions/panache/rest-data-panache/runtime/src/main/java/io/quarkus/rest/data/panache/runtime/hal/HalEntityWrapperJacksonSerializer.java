package io.quarkus.rest.data.panache.runtime.hal;

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

public class HalEntityWrapperJacksonSerializer extends JsonSerializer<HalEntityWrapper> {

    private final HalLinksProvider linksExtractor;

    public HalEntityWrapperJacksonSerializer() {
        this.linksExtractor = new RestEasyHalLinksProvider();
    }

    HalEntityWrapperJacksonSerializer(HalLinksProvider linksExtractor) {
        this.linksExtractor = linksExtractor;
    }

    @Override
    public void serialize(HalEntityWrapper wrapper, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {

        generator.writeStartObject();
        for (BeanPropertyDefinition property : getPropertyDefinitions(serializers, wrapper.getEntity().getClass())) {
            AnnotatedMember accessor = property.getAccessor();
            if (accessor != null) {
                Object value = accessor.getValue(wrapper.getEntity());
                generator.writeFieldName(property.getName());
                if (value == null) {
                    generator.writeNull();
                } else {
                    serializers.findValueSerializer(value.getClass()).serialize(value, generator, serializers);
                }
            }
        }
        writeLinks(wrapper.getEntity(), generator);
        generator.writeEndObject();
    }

    private void writeLinks(Object entity, JsonGenerator generator) throws IOException {
        Map<String, HalLink> links = linksExtractor.getLinks(entity);
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
