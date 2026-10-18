package io.quarkus.hal;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

public class HalLinkJsonbSerializer implements JsonbSerializer<HalLink> {

    @Override
    public void serialize(HalLink value, JsonGenerator generator, SerializationContext context) {
        generator.writeStartObject();
        generator.write("href", value.getHref());
        if (value.getTitle() != null) {
            generator.write("title", value.getTitle());
        }

        if (value.getType() != null) {
            generator.write("type", value.getType());
        }

        generator.writeEnd();
    }
}
