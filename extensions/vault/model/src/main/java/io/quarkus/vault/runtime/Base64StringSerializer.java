package io.quarkus.vault.runtime;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class Base64StringSerializer extends StdSerializer<Base64String> {

    public Base64StringSerializer() {
        this(null);
    }

    public Base64StringSerializer(Class<Base64String> vc) {
        super(vc);
    }

    @Override
    public void serialize(Base64String base64String, JsonGenerator jgen, SerializerProvider serializerProvider)
            throws IOException {
        jgen.writeString(base64String.getValue());
    }
}
