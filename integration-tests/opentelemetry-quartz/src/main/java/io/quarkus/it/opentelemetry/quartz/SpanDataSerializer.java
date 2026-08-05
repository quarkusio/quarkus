package io.quarkus.it.opentelemetry.quartz;

import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class SpanDataSerializer extends StdSerializer<SpanData> {

    public SpanDataSerializer() {
        this(null);
    }

    public SpanDataSerializer(Class<SpanData> type) {
        super(type);
    }

    @Override
    public void serialize(SpanData spanData, JsonGenerator gen, SerializationContext serializationContext) {
        gen.writeStartObject();

        gen.writeStringProperty("name", spanData.getName());
        gen.writeStringProperty("kind", spanData.getKind().name());
        gen.writeNumberProperty("startEpochNanos", spanData.getStartEpochNanos());
        gen.writeNumberProperty("endEpochNanos", spanData.getEndEpochNanos());

        gen.writeObjectPropertyStart("status");
        gen.writeStringProperty("statusCode", spanData.getStatus().getStatusCode().name());
        gen.writeEndObject();

        gen.writeArrayPropertyStart("events");
        for (EventData event : spanData.getEvents()) {
            gen.writeStartObject();
            if (event instanceof ExceptionEventData) {
                ExceptionEventData exEvent = (ExceptionEventData) event;
                gen.writeObjectPropertyStart("exception");
                gen.writeStringProperty("message", exEvent.getException().getMessage());
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }
        gen.writeEndArray();

        gen.writeEndObject();
    }
}
