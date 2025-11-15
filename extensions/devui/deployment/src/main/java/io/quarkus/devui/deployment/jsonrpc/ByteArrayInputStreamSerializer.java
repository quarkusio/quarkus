package io.quarkus.devui.deployment.jsonrpc;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_ENCODER;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.quarkus.deployment.util.IoUtil;

public class ByteArrayInputStreamSerializer extends JsonSerializer<ByteArrayInputStream> {

    @Override
    public void serialize(ByteArrayInputStream value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        byte[] readBytes = IoUtil.readBytes(value);
        jgen.writeString(BASE64_ENCODER.encodeToString(readBytes));
    }
}
