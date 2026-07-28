package io.quarkus.devui.deployment.jsonrpc;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_ENCODER;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import io.quarkus.deployment.util.IoUtil;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class ByteArrayInputStreamSerializer extends StdSerializer<ByteArrayInputStream> {

    public ByteArrayInputStreamSerializer() {
        super(ByteArrayInputStream.class);
    }

    @Override
    public void serialize(ByteArrayInputStream value, JsonGenerator jgen, SerializationContext provider) {
        try {
            byte[] readBytes = IoUtil.readBytes(value);
            jgen.writeString(BASE64_ENCODER.encodeToString(readBytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
