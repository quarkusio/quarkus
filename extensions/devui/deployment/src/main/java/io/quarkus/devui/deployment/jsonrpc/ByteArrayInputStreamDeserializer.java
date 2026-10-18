package io.quarkus.devui.deployment.jsonrpc;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_DECODER;

import java.io.ByteArrayInputStream;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public class ByteArrayInputStreamDeserializer extends StdDeserializer<ByteArrayInputStream> {

    public ByteArrayInputStreamDeserializer() {
        super(ByteArrayInputStream.class);
    }

    @Override
    public ByteArrayInputStream deserialize(JsonParser p, DeserializationContext ctxt) {
        String text = p.getString();
        try {
            byte[] decode = BASE64_DECODER.decode(text);
            return new ByteArrayInputStream(decode);
        } catch (IllegalArgumentException e) {
            throw InvalidFormatException.from(p, "Expected a base64 encoded byte array", text, ByteArrayInputStream.class);
        }
    }
}
