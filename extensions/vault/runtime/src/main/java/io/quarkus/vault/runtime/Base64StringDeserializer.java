package io.quarkus.vault.runtime;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class Base64StringDeserializer extends StdDeserializer<Base64String> {

    public Base64StringDeserializer() {
        this(null);
    }

    public Base64StringDeserializer(Class<Base64String> vc) {
        super(vc);
    }

    @Override
    public Base64String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        return new Base64String(jsonParser.getText());
    }
}
