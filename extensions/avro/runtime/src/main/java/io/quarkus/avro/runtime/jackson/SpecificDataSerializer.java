package io.quarkus.avro.runtime.jackson;

import java.io.IOException;

import org.apache.avro.specific.SpecificData;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class SpecificDataSerializer extends StdSerializer<SpecificData> {
    public SpecificDataSerializer() {
        super(SpecificData.class);
    }

    @Override
    public void serialize(SpecificData data, JsonGenerator jsonGenerator, SerializationContext serializerProvider)
            throws IOException {
        System.out.println("here! ");
        // Skip the specific data instance
    }

}
