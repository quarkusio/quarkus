package io.quarkus.hal;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class HalLinkJacksonSerializer extends ValueSerializer<HalLink> {

    @Override
    public void serialize(HalLink value, JsonGenerator generator, SerializationContext serializers) {
        generator.writeStartObject();
        generator.writePOJOProperty("href", value.getHref());
        if (value.getTitle() != null) {
            generator.writePOJOProperty("title", value.getTitle());
        }

        if (value.getType() != null) {
            generator.writePOJOProperty("type", value.getType());
        }

        generator.writeEndObject();
    }
}
