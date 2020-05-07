package io.quarkus.panache.rest.common.runtime.hal;

import java.util.Map;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

import io.quarkus.panache.rest.common.runtime.utils.StringUtil;

public class HalCollectionWrapperJsonbSerializer implements JsonbSerializer<HalCollectionWrapper> {

    private final HalLinksExtractor linksExtractor;

    public HalCollectionWrapperJsonbSerializer() {
        this.linksExtractor = new RestEasyHalLinksExtractor();
    }

    HalCollectionWrapperJsonbSerializer(HalLinksExtractor linksExtractor) {
        this.linksExtractor = linksExtractor;
    }

    @Override
    public void serialize(HalCollectionWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        generator.writeStartObject();
        writeEmbedded(wrapper, generator, context);
        writeLinks(wrapper.getElementType(), generator, context);
        generator.writeEnd();
    }

    private void writeEmbedded(HalCollectionWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        generator.writeKey("_embedded");
        generator.writeStartObject();
        generator.writeKey(typeToFieldName(wrapper.getElementType().getSimpleName()));
        generator.writeStartArray();
        for (Object entity : wrapper.getCollection()) {
            context.serialize(new HalEntityWrapper(entity), generator);
        }
        generator.writeEnd();
        generator.writeEnd();
    }

    private void writeLinks(Class<?> type, JsonGenerator generator, SerializationContext context) {
        Map<String, HalLink> links = linksExtractor.getLinks(type);
        context.serialize("_links", links, generator);
    }

    private String typeToFieldName(String typeName) {
        String[] pieces = StringUtil.camelToHyphenated(typeName).split("-");
        pieces[pieces.length - 1] = StringUtil.toPlural(pieces[pieces.length - 1]);
        return String.join("-", pieces);
    }
}
