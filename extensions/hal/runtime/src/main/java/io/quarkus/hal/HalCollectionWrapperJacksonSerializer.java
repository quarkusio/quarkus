package io.quarkus.hal;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class HalCollectionWrapperJacksonSerializer extends ValueSerializer<HalCollectionWrapper<?>> {

    @Override
    public void serialize(HalCollectionWrapper<?> wrapper, JsonGenerator generator, SerializationContext serializers) {
        generator.writeStartObject();
        writeEmbedded(wrapper, generator, serializers);
        writeLinks(wrapper, generator);
        generator.writeEndObject();
    }

    private void writeEmbedded(HalCollectionWrapper<?> wrapper, JsonGenerator generator,
            SerializationContext serializers) {
        ValueSerializer<Object> entitySerializer = serializers.findValueSerializer(HalEntityWrapper.class);

        generator.writeName("_embedded");
        generator.writeStartObject();
        generator.writeName(wrapper.getCollectionName());
        generator.writeStartArray();
        for (HalEntityWrapper<?> entity : wrapper.getCollection()) {
            entitySerializer.serialize(entity, generator, serializers);
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeLinks(HalCollectionWrapper<?> wrapper, JsonGenerator generator) {
        generator.writeName("_links");
        generator.writePOJO(wrapper.getLinks());
    }
}
