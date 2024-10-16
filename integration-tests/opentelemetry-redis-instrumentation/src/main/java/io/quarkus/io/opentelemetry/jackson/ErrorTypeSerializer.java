package io.quarkus.io.opentelemetry.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.vertx.redis.client.impl.types.ErrorType;

public class ErrorTypeSerializer extends StdSerializer<ErrorType> {
    public ErrorTypeSerializer() {
        super(ErrorType.class);
    }

    @Override
    public void serialize(ErrorType errorType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", errorType.type().name());
        jsonGenerator.writeStringField("message", errorType.getMessage());
        jsonGenerator.writeEndObject();
    }
}
