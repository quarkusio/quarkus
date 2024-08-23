package io.quarkus.avro.runtime.jackson;

import java.io.IOException;

import org.apache.avro.specific.SpecificData;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class SpecificDataSerializer extends StdSerializer<SpecificData> {
    public SpecificDataSerializer() {
        super(SpecificData.class);
    }

    @Override
    public void serialize(SpecificData data, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        System.out.println("here! ");
        // Skip the specific data instance
    }

}
