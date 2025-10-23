package io.quarkus.hal;

import java.io.IOException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

public class HalLinkJacksonSerializer extends ValueSerializer<HalLink> {

    @Override
    public void serialize(HalLink value, JsonGenerator generator, SerializationContext serializers) throws IOException {
        generator.writeStartObject();
        generator.writeObjectField("href", value.getHref());
        if (value.getTitle() != null) {
            generator.writeObjectField("title", value.getTitle());
        }

        if (value.getType() != null) {
            generator.writeObjectField("type", value.getType());
        }

        generator.writeEndObject();
    }
}
