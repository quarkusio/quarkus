package io.quarkus.devui.deployment.jsonrpc;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_ENCODER;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import io.quarkus.deployment.util.IoUtil;

public class ByteArrayInputStreamSerializer extends ValueSerializer<ByteArrayInputStream> {

    @Override
    public void serialize(ByteArrayInputStream value, JsonGenerator jgen, SerializationContext provider) throws IOException {
        byte[] readBytes = IoUtil.readBytes(value);
        jgen.writeString(BASE64_ENCODER.encodeToString(readBytes));
    }
}
