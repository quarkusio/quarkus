package io.quarkus.io.opentelemetry.jackson;

import io.vertx.redis.client.impl.types.ErrorType;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class ErrorTypeSerializer extends StdSerializer<ErrorType> {
    public ErrorTypeSerializer() {
        super(ErrorType.class);
    }

    @Override
    public void serialize(ErrorType errorType, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringProperty("type", errorType.type().name());
        jsonGenerator.writeStringProperty("message", errorType.getMessage());
        jsonGenerator.writeEndObject();
    }
}
