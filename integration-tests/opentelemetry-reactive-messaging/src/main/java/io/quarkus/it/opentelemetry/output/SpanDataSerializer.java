package io.quarkus.it.opentelemetry.output;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.opentelemetry.sdk.trace.data.SpanData;

public class SpanDataSerializer extends StdSerializer<SpanData> {
    public SpanDataSerializer() {
        this(null);
    }

    public SpanDataSerializer(Class<SpanData> type) {
        super(type);
    }

    @Override
    public void serialize(SpanData spanData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("spanId", spanData.getSpanId());
        jsonGenerator.writeStringField("traceId", spanData.getTraceId());
        jsonGenerator.writeStringField("name", spanData.getName());
        jsonGenerator.writeStringField("kind", spanData.getKind().name());
        jsonGenerator.writeBooleanField("ended", spanData.hasEnded());

        jsonGenerator.writeStringField("parent_spanId", spanData.getParentSpanContext().getSpanId());
        jsonGenerator.writeStringField("parent_traceId", spanData.getParentSpanContext().getTraceId());
        jsonGenerator.writeBooleanField("parent_remote", spanData.getParentSpanContext().isRemote());
        jsonGenerator.writeBooleanField("parent_valid", spanData.getParentSpanContext().isValid());

        spanData.getAttributes().forEach((k, v) -> {
            try {
                jsonGenerator.writeStringField("attr_" + k.getKey(), v.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        spanData.getResource().getAttributes().forEach((k, v) -> {
            try {
                jsonGenerator.writeStringField("resource_" + k.getKey(), v.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        jsonGenerator.writeEndObject();
    }
}
