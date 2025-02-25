package io.quarkus.it.opentelemetry.output;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.opentelemetry.sdk.logs.data.LogRecordData;

public class LogRecordDataSerializer extends StdSerializer<LogRecordData> {

    public LogRecordDataSerializer() {
        this(null);
    }

    public LogRecordDataSerializer(Class<LogRecordData> type) {
        super(type);
    }

    @Override
    public void serialize(LogRecordData logRecordData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("severityText", logRecordData.getSeverityText());
        jsonGenerator.writeObjectField("spanContext", logRecordData.getSpanContext());
        jsonGenerator.writeStringField("body_body", logRecordData.getBody().asString());

        logRecordData.getAttributes().forEach((k, v) -> {
            try {
                jsonGenerator.writeStringField("attr_" + k.getKey(), v.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        logRecordData.getResource().getAttributes().forEach((k, v) -> {
            try {
                jsonGenerator.writeStringField("resource_" + k.getKey(), v.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        jsonGenerator.writeEndObject();
    }
}
