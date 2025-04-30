package io.quarkus.hal;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

// Using the raw type here as eclipse yasson doesn't like custom serializers for
// generic root types, see https://github.com/eclipse-ee4j/yasson/issues/639
public class HalCollectionWrapperJsonbSerializer implements JsonbSerializer<HalCollectionWrapper> {

    @Override
    public void serialize(HalCollectionWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        generator.writeStartObject();
        writeEmbedded(wrapper, generator, context);
        writeLinks(wrapper, generator, context);
        generator.writeEnd();
    }

    private void writeEmbedded(HalCollectionWrapper<?> wrapper, JsonGenerator generator, SerializationContext context) {
        generator.writeKey("_embedded");
        generator.writeStartObject();
        generator.writeKey(wrapper.getCollectionName());
        generator.writeStartArray();
        for (HalEntityWrapper<?> entity : wrapper.getCollection()) {
            context.serialize(entity, generator);
        }
        generator.writeEnd();
        generator.writeEnd();
    }

    private void writeLinks(HalCollectionWrapper<?> wrapper, JsonGenerator generator, SerializationContext context) {
        context.serialize("_links", wrapper.getLinks(), generator);
    }
}
