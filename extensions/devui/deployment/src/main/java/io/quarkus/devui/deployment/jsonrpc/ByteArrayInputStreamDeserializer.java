package io.quarkus.devui.deployment.jsonrpc;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_DECODER;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class ByteArrayInputStreamDeserializer extends JsonDeserializer<ByteArrayInputStream> {

    @Override
    public ByteArrayInputStream deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        String text = p.getText();
        try {
            byte[] decode = BASE64_DECODER.decode(text);
            return new ByteArrayInputStream(decode);
        } catch (IllegalArgumentException e) {
            throw new InvalidFormatException(p, "Expected a base64 encoded byte array", text, ByteArrayInputStream.class);
        }
    }
}
