package io.quarkus.devui.deployment.jsonrpc;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_DECODER;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public class ByteArrayInputStreamDeserializer extends ValueDeserializer<ByteArrayInputStream> {

    @Override
    public ByteArrayInputStream deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JacksonException {
        String text = p.getText();
        try {
            byte[] decode = BASE64_DECODER.decode(text);
            return new ByteArrayInputStream(decode);
        } catch (IllegalArgumentException e) {
            throw new InvalidFormatException(p, "Expected a base64 encoded byte array", text, ByteArrayInputStream.class);
        }
    }
}
