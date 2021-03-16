package io.quarkus.rest.data.panache.runtime.hal;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class HalCollectionWrapperJacksonSerializer extends JsonSerializer<HalCollectionWrapper> {

    private final HalLinksProvider linksExtractor;

    public HalCollectionWrapperJacksonSerializer() {
        this.linksExtractor = new RestEasyHalLinksProvider();
    }

    HalCollectionWrapperJacksonSerializer(HalLinksProvider linksExtractor) {
        this.linksExtractor = linksExtractor;
    }

    @Override
    public void serialize(HalCollectionWrapper wrapper, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeStartObject();
        writeEmbedded(wrapper, generator, serializers);
        writeLinks(wrapper, generator);
        generator.writeEndObject();
    }

    private void writeEmbedded(HalCollectionWrapper wrapper, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        JsonSerializer<Object> entitySerializer = serializers.findValueSerializer(HalEntityWrapper.class);

        generator.writeFieldName("_embedded");
        generator.writeStartObject();
        generator.writeFieldName(wrapper.getCollectionName());
        generator.writeStartArray(wrapper.getCollection().size());
        for (Object entity : wrapper.getCollection()) {
            entitySerializer.serialize(new HalEntityWrapper(entity), generator, serializers);
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeLinks(HalCollectionWrapper wrapper, JsonGenerator generator) throws IOException {
        Map<String, HalLink> links = linksExtractor.getLinks(wrapper.getElementType());
        links.putAll(wrapper.getLinks());
        generator.writeFieldName("_links");
        generator.writeObject(links);
    }
}
