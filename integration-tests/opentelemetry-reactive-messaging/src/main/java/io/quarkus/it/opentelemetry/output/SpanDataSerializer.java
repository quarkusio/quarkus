package io.quarkus.it.opentelemetry.output;

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
    public void serialize(SpanData spanData, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringProperty("spanId", spanData.getSpanId());
        jsonGenerator.writeStringProperty("traceId", spanData.getTraceId());
        jsonGenerator.writeStringProperty("name", spanData.getName());
        jsonGenerator.writeStringProperty("kind", spanData.getKind().name());
        jsonGenerator.writeBooleanProperty("ended", spanData.hasEnded());

        jsonGenerator.writeStringProperty("parent_spanId", spanData.getParentSpanContext().getSpanId());
        jsonGenerator.writeStringProperty("parent_traceId", spanData.getParentSpanContext().getTraceId());
        jsonGenerator.writeBooleanProperty("parent_remote", spanData.getParentSpanContext().isRemote());
        jsonGenerator.writeBooleanProperty("parent_valid", spanData.getParentSpanContext().isValid());

        spanData.getAttributes().forEach((k, v) -> {
            jsonGenerator.writeStringProperty("attr_" + k.getKey(), v.toString());
        });

        spanData.getResource().getAttributes().forEach((k, v) -> {
            jsonGenerator.writeStringProperty("resource_" + k.getKey(), v.toString());
        });

        jsonGenerator.writeEndObject();
    }
}
