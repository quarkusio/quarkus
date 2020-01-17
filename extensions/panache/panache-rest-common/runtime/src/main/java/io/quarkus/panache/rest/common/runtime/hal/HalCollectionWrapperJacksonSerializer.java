package io.quarkus.panache.rest.common.runtime.hal;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.quarkus.panache.rest.common.runtime.utils.StringUtil;

public class HalCollectionWrapperJacksonSerializer extends JsonSerializer<HalCollectionWrapper> {

    private final HalLinksExtractor linksExtractor;

    public HalCollectionWrapperJacksonSerializer() {
        this.linksExtractor = new RestEasyHalLinksExtractor();
    }

    HalCollectionWrapperJacksonSerializer(HalLinksExtractor linksExtractor) {
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
        generator.writeFieldName(typeToFieldName(wrapper.getElementType().getSimpleName()));
        generator.writeStartArray(wrapper.getCollection().size());
        for (Object entity : wrapper.getCollection()) {
            entitySerializer.serialize(new HalEntityWrapper(entity), generator, serializers);
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeLinks(HalCollectionWrapper wrapper, JsonGenerator generator) throws IOException {
        Map<String, HalLink> links = linksExtractor.getLinks(wrapper.getElementType());
        generator.writeFieldName("_links");
        generator.writeObject(links);
    }

    private String typeToFieldName(String typeName) {
        String[] pieces = StringUtil.camelToHyphenated(typeName).split("-");
        pieces[pieces.length - 1] = StringUtil.toPlural(pieces[pieces.length - 1]);
        return String.join("-", pieces);
    }
}
