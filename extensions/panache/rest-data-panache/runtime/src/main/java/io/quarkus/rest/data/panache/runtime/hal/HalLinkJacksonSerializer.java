package io.quarkus.rest.data.panache.runtime.hal;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class HalLinkJacksonSerializer extends JsonSerializer<HalLink> {

    @Override
    public void serialize(HalLink value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeStartObject();
        generator.writeObjectField("href", value.getHref());
        generator.writeEndObject();
    }
}
