package io.quarkus.hal;

import java.io.IOException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

public class HalCollectionWrapperJacksonSerializer extends ValueSerializer<HalCollectionWrapper<?>> {

    @Override
    public void serialize(HalCollectionWrapper<?> wrapper, JsonGenerator generator, SerializationContext serializers)
            throws IOException {
        generator.writeStartObject();
        writeEmbedded(wrapper, generator, serializers);
        writeLinks(wrapper, generator);
        generator.writeEndObject();
    }

    private void writeEmbedded(HalCollectionWrapper<?> wrapper, JsonGenerator generator, SerializationContext serializers)
            throws IOException {
        ValueSerializer<Object> entitySerializer = serializers.findValueSerializer(HalEntityWrapper.class);

        generator.writeFieldName("_embedded");
        generator.writeStartObject();
        generator.writeFieldName(wrapper.getCollectionName());
        generator.writeStartArray();
        for (HalEntityWrapper<?> entity : wrapper.getCollection()) {
            entitySerializer.serialize(entity, generator, serializers);
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeLinks(HalCollectionWrapper<?> wrapper, JsonGenerator generator) throws IOException {
        generator.writeFieldName("_links");
        generator.writeObject(wrapper.getLinks());
    }
}
