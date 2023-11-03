package io.quarkus.hal;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

public class HalCollectionWrapperJsonbSerializer implements JsonbSerializer<HalCollectionWrapper> {

    @Override
    public void serialize(HalCollectionWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        generator.writeStartObject();
        writeEmbedded(wrapper, generator, context);
        writeLinks(wrapper, generator, context);
        generator.writeEnd();
    }

    private void writeEmbedded(HalCollectionWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        generator.writeKey("_embedded");
        generator.writeStartObject();
        generator.writeKey(wrapper.getCollectionName());
        generator.writeStartArray();
        for (HalEntityWrapper entity : wrapper.getCollection()) {
            context.serialize(entity, generator);
        }
        generator.writeEnd();
        generator.writeEnd();
    }

    private void writeLinks(HalCollectionWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        context.serialize("_links", wrapper.getLinks(), generator);
    }
}
