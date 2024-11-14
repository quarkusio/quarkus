package io.quarkus.hal;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class HalLinkJacksonSerializer extends JsonSerializer<HalLink> {

    @Override
    public void serialize(HalLink value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
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
