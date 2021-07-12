package io.quarkus.it.opentracing.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.opentracing.mock.MockSpan;

public class MockSpanSerializer extends StdSerializer<MockSpan> {

    public MockSpanSerializer() {
        super(MockSpan.class);
    }

    @Override
    public void serialize(MockSpan mockSpan, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("spanId", mockSpan.context().toSpanId());
        jsonGenerator.writeStringField("traceId", mockSpan.context().toTraceId());
        jsonGenerator.writeStringField("operation_name", mockSpan.operationName());

        jsonGenerator.writeNumberField("parent_spanId", mockSpan.parentId());

        mockSpan.tags().forEach((k, v) -> {
            try {
                jsonGenerator.writeStringField("tag_" + k, v.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        mockSpan.context().baggageItems().forEach(entry -> {
            try {
                jsonGenerator.writeStringField("baggage_" + entry.getKey(), entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        jsonGenerator.writeEndObject();
    }
}
