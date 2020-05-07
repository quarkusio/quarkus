package io.quarkus.panache.rest.common.runtime.hal;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

public class HalLinkJsonbSerializer implements JsonbSerializer<HalLink> {

    @Override
    public void serialize(HalLink value, JsonGenerator generator, SerializationContext context) {
        generator.writeStartObject();
        generator.write("href", value.getHref());
        generator.writeEnd();
    }
}
