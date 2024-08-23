package io.quarkus.hal;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class HalCollectionWrapperJacksonSerializer extends JsonSerializer<HalCollectionWrapper<?>> {

    @Override
    public void serialize(HalCollectionWrapper<?> wrapper, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeStartObject();
        writeEmbedded(wrapper, generator, serializers);
        writeLinks(wrapper, generator);
        generator.writeEndObject();
    }

    private void writeEmbedded(HalCollectionWrapper<?> wrapper, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        JsonSerializer<Object> entitySerializer = serializers.findValueSerializer(HalEntityWrapper.class);

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
