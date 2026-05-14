package io.quarkus.it.opentelemetry.scheduler;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.SpanData;

public class SpanDataSerializer extends StdSerializer<SpanData> {

    public SpanDataSerializer() {
        this(null);
    }

    public SpanDataSerializer(Class<SpanData> type) {
        super(type);
    }

    @Override
    public void serialize(SpanData spanData, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();

        gen.writeStringField("name", spanData.getName());
        gen.writeStringField("kind", spanData.getKind().name());
        gen.writeNumberField("startEpochNanos", spanData.getStartEpochNanos());
        gen.writeNumberField("endEpochNanos", spanData.getEndEpochNanos());

        gen.writeObjectFieldStart("status");
        gen.writeStringField("statusCode", spanData.getStatus().getStatusCode().name());
        gen.writeEndObject();

        gen.writeArrayFieldStart("events");
        for (EventData event : spanData.getEvents()) {
            gen.writeStartObject();
            if (event instanceof ExceptionEventData) {
                ExceptionEventData exEvent = (ExceptionEventData) event;
                gen.writeObjectFieldStart("exception");
                gen.writeStringField("message", exEvent.getException().getMessage());
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }
        gen.writeEndArray();

        gen.writeEndObject();
    }
}
